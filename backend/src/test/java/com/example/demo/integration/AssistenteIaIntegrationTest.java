package com.example.demo.integration;

import com.example.demo.enums.PlanoSaas;
import com.example.demo.enums.StatusAssinaturaSaas;
import com.example.demo.model.Barbearia;
import com.example.demo.repository.BarbeariaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O Assistente de IA é um recurso Premium (ver Recurso.ASSISTENTE_IA): só ADMIN/GERENTE
 * conseguem usar, e só a partir do plano Premium (fora do trial gratuito, que libera tudo).
 * Sem ANTHROPIC_API_KEY configurada (é o caso nos testes), a resposta é um aviso amigável —
 * nunca uma tentativa real de chamar a API, o que custaria dinheiro a cada rodada de teste.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AssistenteIaIntegrationTest {

    private record Sessao(String token, Long barbeariaId) {}

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BarbeariaRepository barbeariaRepository;

    private Sessao registrar(String email) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeBarbearia":"Barbearia Teste","nomeAdmin":"Admin","email":"%s","cpf":"%s","senha":"123456"}
                                """.formatted(email, com.example.demo.util.CpfTestFixture.gerar(email))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString());
        return new Sessao(json.get("token").asText(), json.get("barbeariaId").asLong());
    }

    private void ativarPlano(Long barbeariaId, PlanoSaas plano) {
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId).orElseThrow();
        barbearia.setStatusAssinaturaSaas(StatusAssinaturaSaas.ATIVA);
        barbearia.setPlanoSaas(plano);
        barbeariaRepository.save(barbearia);
    }

    private ResultActionsHelper perguntar(String token, String pergunta) throws Exception {
        return new ResultActionsHelper(mockMvc.perform(post("/api/assistente/perguntar")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pergunta\":\"%s\"}".formatted(pergunta))));
    }

    private record ResultActionsHelper(org.springframework.test.web.servlet.ResultActions acoes) {}

    @Test
    void duranteOTrialConsegueChamarOAssistenteMesmoSemPlanoPremium() throws Exception {
        Sessao sessao = registrar("trial-ia@teste.com");

        perguntar(sessao.token(), "Quanto faturei hoje?").acoes()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resposta").value(
                        org.hamcrest.Matchers.containsString("ainda não foi configurado")));
    }

    @Test
    void planoBasicoAtivoBloqueiaOAssistenteExigindoPremium() throws Exception {
        Sessao sessao = registrar("basico-ia@teste.com");
        ativarPlano(sessao.barbeariaId(), PlanoSaas.BASICO);

        perguntar(sessao.token(), "Quanto faturei hoje?").acoes()
                .andExpect(status().is(402))
                .andExpect(jsonPath("$.upgradeNecessario").value(true))
                .andExpect(jsonPath("$.recurso").value("ASSISTENTE_IA"))
                .andExpect(jsonPath("$.planoNecessario").value("PREMIUM"));
    }

    @Test
    void planoProfissionalAtivoAindaNaoDaAcessoAoAssistente() throws Exception {
        Sessao sessao = registrar("profissional-plano-ia@teste.com");
        ativarPlano(sessao.barbeariaId(), PlanoSaas.PROFISSIONAL);

        perguntar(sessao.token(), "Quanto faturei hoje?").acoes()
                .andExpect(status().is(402))
                .andExpect(jsonPath("$.planoNecessario").value("PREMIUM"));
    }

    @Test
    void planoPremiumAtivoLiberaOAssistente() throws Exception {
        Sessao sessao = registrar("premium-ia@teste.com");
        ativarPlano(sessao.barbeariaId(), PlanoSaas.PREMIUM);

        perguntar(sessao.token(), "Quanto faturei hoje?").acoes()
                .andExpect(status().isOk());
    }

    @Test
    void perguntaEmBrancoRetorna400() throws Exception {
        Sessao sessao = registrar("pergunta-vazia-ia@teste.com");

        perguntar(sessao.token(), "").acoes()
                .andExpect(status().isBadRequest());
    }
}
