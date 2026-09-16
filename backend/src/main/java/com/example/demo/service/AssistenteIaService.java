package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Orquestra a conversa com a API da Anthropic (Claude): manda a pergunta + o catálogo de
 * ferramentas, executa as ferramentas que o modelo pedir (sempre via FerramentasAssistenteService,
 * nunca acesso direto ao banco daqui) e devolve a resposta final em texto.
 *
 * Nunca envia a pergunta pra IA sem o prompt de sistema que proíbe inventar números — todo
 * dado numérico da resposta final vem de uma ferramenta chamada de verdade.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AssistenteIaService {

    private static final URI ANTHROPIC_URL = URI.create("https://api.anthropic.com/v1/messages");
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_RODADAS_FERRAMENTAS = 5;

    private static final String PROMPT_SISTEMA = """
            Você é o assistente de gestão do BarberFlow, sistema de gestão para barbearias.
            Responda em português, de forma direta e objetiva, à pergunta do dono/gerente sobre
            os dados do NEGÓCIO DELE.

            REGRAS OBRIGATÓRIAS:
            - Nunca invente números. Todo dado numérico (faturamento, atendimentos, comissões,
              clientes) tem que vir de uma chamada de ferramenta.
            - Se a ferramenta não trouxer a informação pedida, diga claramente que não encontrou
              — nunca tente adivinhar ou estimar.
            - Seja conciso: respostas curtas e diretas, como um contador de confiança faria.
            - Valores em reais (R$), formatados como 1.234,56.
            - Nunca mencione nomes técnicos de ferramentas, tabelas ou IDs internos na resposta.
            """;

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FerramentasAssistenteService ferramentas;

    @Value("${anthropic.api-key:}")
    private String apiKey;

    @Value("${anthropic.model:claude-sonnet-5}")
    private String modelo;

    public String responder(String pergunta) {
        String chave = apiKey == null ? "" : apiKey.trim();
        if (chave.isBlank()) {
            return "O Assistente de IA ainda não foi configurado nesta conta. Peça para o suporte configurar a chave de API.";
        }

        try {
            List<Map<String, Object>> mensagens = new ArrayList<>();
            mensagens.add(Map.of("role", "user", "content", pergunta));

            for (int rodada = 0; rodada < MAX_RODADAS_FERRAMENTAS; rodada++) {
                JsonNode resposta = chamarAnthropic(chave, mensagens);
                String motivoParada = resposta.path("stop_reason").asText("");

                List<Map<String, Object>> conteudoAssistente = new ArrayList<>();
                List<Map<String, Object>> resultadosFerramentas = new ArrayList<>();
                StringBuilder textoFinal = new StringBuilder();

                for (JsonNode bloco : resposta.path("content")) {
                    conteudoAssistente.add(objectMapper.convertValue(bloco, Map.class));
                    String tipo = bloco.path("type").asText("");

                    if ("text".equals(tipo)) {
                        textoFinal.append(bloco.path("text").asText(""));
                    } else if ("tool_use".equals(tipo)) {
                        String nomeFerramenta = bloco.path("name").asText("");
                        String idChamada = bloco.path("id").asText("");
                        Object resultado;
                        try {
                            resultado = executarFerramenta(nomeFerramenta, bloco.path("input"));
                        } catch (Exception e) {
                            log.error("Falha ao executar a ferramenta {} do assistente", nomeFerramenta, e);
                            resultado = Map.of("erro", "Não foi possível consultar esse dado agora.");
                        }
                        resultadosFerramentas.add(Map.of(
                                "type", "tool_result",
                                "tool_use_id", idChamada,
                                "content", objectMapper.writeValueAsString(resultado)
                        ));
                    }
                }

                if (!"tool_use".equals(motivoParada)) {
                    String texto = textoFinal.toString().trim();
                    return texto.isEmpty()
                            ? "Não consegui gerar uma resposta agora. Tente reformular a pergunta."
                            : texto;
                }

                mensagens.add(Map.of("role", "assistant", "content", conteudoAssistente));
                mensagens.add(Map.of("role", "user", "content", resultadosFerramentas));
            }

            return "Essa pergunta ficou complexa demais pra eu responder com segurança agora. Tente perguntar de um jeito mais direto.";
        } catch (Exception e) {
            log.error("Falha ao consultar o Assistente de IA", e);
            return "Não consegui falar com o Assistente de IA agora. Tente novamente em instantes.";
        }
    }

    private JsonNode chamarAnthropic(String chave, List<Map<String, Object>> mensagens) throws Exception {
        Map<String, Object> corpo = Map.of(
                "model", modelo,
                "max_tokens", 1024,
                "system", PROMPT_SISTEMA,
                "messages", mensagens,
                "tools", definicaoFerramentas()
        );

        HttpRequest request = HttpRequest.newBuilder(ANTHROPIC_URL)
                .header("x-api-key", chave)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(corpo)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Anthropic respondeu " + response.statusCode() + ": " + response.body());
        }
        return objectMapper.readTree(response.body());
    }

    private Object executarFerramenta(String nome, JsonNode entrada) {
        String periodo = entrada.path("periodo").asText("mes_atual");
        return switch (nome) {
            case "obter_faturamento" -> ferramentas.obterFaturamento(periodo);
            case "obter_atendimentos" -> ferramentas.obterAtendimentos(periodo);
            case "obter_comissoes" -> ferramentas.obterComissoes(periodo);
            case "obter_despesas" -> ferramentas.obterDespesas(periodo);
            case "obter_clientes" -> ferramentas.obterClientes();
            case "obter_profissionais" -> ferramentas.obterProfissionais();
            case "obter_servicos" -> ferramentas.obterServicos();
            default -> Map.of("erro", "Ferramenta desconhecida: " + nome);
        };
    }

    private List<Map<String, Object>> definicaoFerramentas() {
        Map<String, Object> parametroPeriodo = Map.of(
                "type", "object",
                "properties", Map.of(
                        "periodo", Map.of(
                                "type", "string",
                                "enum", List.of("hoje", "ontem", "semana_atual", "semana_passada", "mes_atual", "mes_passado"),
                                "description", "Período de tempo a consultar."
                        )
                ),
                "required", List.of("periodo")
        );
        Map<String, Object> semParametros = Map.of("type", "object", "properties", Map.of());

        return List.of(
                Map.of("name", "obter_faturamento", "description",
                        "Faturamento (receitas recebidas) da barbearia num período: total, ticket médio, valor por forma de pagamento e faturamento por dia.",
                        "input_schema", parametroPeriodo),
                Map.of("name", "obter_atendimentos", "description",
                        "Quantidade de agendamentos/atendimentos num período, incluindo quantos cada profissional concluiu e os serviços mais realizados.",
                        "input_schema", parametroPeriodo),
                Map.of("name", "obter_comissoes", "description",
                        "Comissão de cada profissional (total, já paga e pendente) num período.",
                        "input_schema", parametroPeriodo),
                Map.of("name", "obter_despesas", "description",
                        "Despesas lançadas num período, total e por categoria.",
                        "input_schema", parametroPeriodo),
                Map.of("name", "obter_clientes", "description",
                        "Total de clientes cadastrados, novos nos últimos 30 dias, e a lista de clientes sem agendamento há 30 dias ou mais.",
                        "input_schema", semParametros),
                Map.of("name", "obter_profissionais", "description",
                        "Lista de profissionais cadastrados (nome, função, percentual de comissão, se está ativo).",
                        "input_schema", semParametros),
                Map.of("name", "obter_servicos", "description",
                        "Lista de serviços cadastrados (nome, preço, duração, se está ativo).",
                        "input_schema", semParametros)
        );
    }
}
