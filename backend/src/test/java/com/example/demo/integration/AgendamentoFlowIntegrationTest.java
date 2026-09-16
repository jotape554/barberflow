package com.example.demo.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre o caminho que mais importa no sistema inteiro: agendar um corte, concluir o
 * atendimento e conferir que a receita e a comissão do profissional nascem sozinhas — e só
 * uma vez, mesmo que "concluir" seja chamado duas vezes. Passa pela stack real (Spring
 * Security, JWT, Hibernate) contra um banco H2 em memória, exatamente como em produção,
 * exceto o Postgres real.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AgendamentoFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String registrarBarbeariaEObterToken(String email) throws Exception {
        String corpo = """
                {"nomeBarbearia":"Barbearia Teste","nomeAdmin":"Admin","email":"%s","senha":"123456"}
                """.formatted(email);

        MvcResult resultado = mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private Long criarServico(String token, String preco, int duracaoMinutos) throws Exception {
        String corpo = """
                {"nome":"Corte","descricao":"Corte simples","preco":%s,"duracaoMinutos":%d,"ativo":true}
                """.formatted(preco, duracaoMinutos);

        MvcResult resultado = mockMvc.perform(post("/api/servicos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long criarProfissional(String token, String percentualComissao) throws Exception {
        String corpo = """
                {"nome":"Carlos","funcao":"Barbeiro","percentualComissao":%s,"ativo":true}
                """.formatted(percentualComissao);

        MvcResult resultado = mockMvc.perform(post("/api/profissionais")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long criarCliente(String token) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria\",\"telefone\":\"11999999999\"}"))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void concluirAgendamentoGeraReceitaEComissaoUmaUnicaVez() throws Exception {
        String token = registrarBarbeariaEObterToken("dono@teste.com");

        Long servicoId = criarServico(token, "50.00", 30);
        Long profissionalId = criarProfissional(token, "40");
        Long clienteId = criarCliente(token);

        LocalDate amanha = LocalDate.now().plusDays(1);
        String corpoAgendamento = """
                {"clienteId":%d,"profissionalId":%d,"servicoId":%d,"data":"%s","horaInicio":"14:00"}
                """.formatted(clienteId, profissionalId, servicoId, amanha);

        MvcResult criacao = mockMvc.perform(post("/api/agendamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAgendamento))
                .andExpect(status().isOk())
                .andReturn();
        Long agendamentoId = objectMapper.readTree(criacao.getResponse().getContentAsString()).get("id").asLong();

        // concluir a primeira vez: deve gerar receita (R$50) e comissão (40% = R$20)
        mockMvc.perform(patch("/api/agendamentos/{id}/concluir", agendamentoId)
                        .header("Authorization", "Bearer " + token)
                        .param("formaPagamento", "PIX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"));

        // concluir de novo: NÃO pode duplicar a receita/comissão
        mockMvc.perform(patch("/api/agendamentos/{id}/concluir", agendamentoId)
                        .header("Authorization", "Bearer " + token)
                        .param("formaPagamento", "PIX"))
                .andExpect(status().isOk());

        MvcResult receitasResultado = mockMvc.perform(get("/api/receitas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode receitas = objectMapper.readTree(receitasResultado.getResponse().getContentAsString());
        assertThat(receitas).hasSize(1);
        assertThat(receitas.get(0).get("valor").asDouble()).isEqualTo(50.00);
        assertThat(receitas.get(0).get("formaPagamento").asText()).isEqualTo("PIX");

        MvcResult comissoesResultado = mockMvc.perform(get("/api/comissoes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode comissoes = objectMapper.readTree(comissoesResultado.getResponse().getContentAsString());
        assertThat(comissoes).hasSize(1);
        assertThat(comissoes.get(0).get("valor").asDouble()).isEqualTo(20.00);
        assertThat(comissoes.get(0).get("paga").asBoolean()).isFalse();

        MvcResult dashboardResultado = mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode dashboard = objectMapper.readTree(dashboardResultado.getResponse().getContentAsString());
        assertThat(dashboard.get("agendamentosConcluidosHoje").asInt()).isEqualTo(0); // agendamento foi para amanhã, não "hoje"
    }

    @Test
    void naoPermiteConcluirAgendamentoCancelado() throws Exception {
        String token = registrarBarbeariaEObterToken("outro-dono@teste.com");
        Long servicoId = criarServico(token, "30.00", 20);
        Long profissionalId = criarProfissional(token, "50");
        Long clienteId = criarCliente(token);

        LocalDate amanha = LocalDate.now().plusDays(1);
        MvcResult criacao = mockMvc.perform(post("/api/agendamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clienteId":%d,"profissionalId":%d,"servicoId":%d,"data":"%s","horaInicio":"09:00"}
                                """.formatted(clienteId, profissionalId, servicoId, amanha)))
                .andExpect(status().isOk())
                .andReturn();
        Long agendamentoId = objectMapper.readTree(criacao.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/agendamentos/{id}/status", agendamentoId)
                        .header("Authorization", "Bearer " + token)
                        .param("status", "CANCELADO"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/agendamentos/{id}/concluir", agendamentoId)
                        .header("Authorization", "Bearer " + token)
                        .param("formaPagamento", "PIX"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void barbeariasDiferentesNaoEnxergamDadosUmaDaOutra() throws Exception {
        String tokenA = registrarBarbeariaEObterToken("barbearia-a@teste.com");
        String tokenB = registrarBarbeariaEObterToken("barbearia-b@teste.com");

        criarCliente(tokenA);

        MvcResult clientesDeB = mockMvc.perform(get("/api/clientes")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode clientes = objectMapper.readTree(clientesDeB.getResponse().getContentAsString());
        assertThat(clientes.get("content")).isEmpty();
    }
}
