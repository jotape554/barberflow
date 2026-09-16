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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dashboard e Financeiro (receitas/despesas) exigem o plano Profissional ou superior — mas só
 * depois que a assinatura está de fato ATIVA. Durante o trial grátis, tudo fica liberado pra
 * a barbearia poder conhecer o sistema por inteiro antes de escolher um plano.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class RecursoPorPlanoIntegrationTest {

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

    @Test
    void duranteOTrialDashboardEFinanceiroFicamLiberados() throws Exception {
        Sessao sessao = registrar("trial-recursos@teste.com");

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + sessao.token()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/receitas").header("Authorization", "Bearer " + sessao.token()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/despesas").header("Authorization", "Bearer " + sessao.token()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/comissoes").header("Authorization", "Bearer " + sessao.token()))
                .andExpect(status().isOk());
    }

    @Test
    void planoBasicoAtivoBloqueiaDashboardFinanceiroEComissoesComRespostaElegante() throws Exception {
        Sessao sessao = registrar("basico-recursos@teste.com");
        ativarPlano(sessao.barbeariaId(), PlanoSaas.BASICO);

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + sessao.token()))
                .andExpect(status().is(402))
                .andExpect(jsonPath("$.upgradeNecessario").value(true))
                .andExpect(jsonPath("$.recurso").value("DASHBOARD"))
                .andExpect(jsonPath("$.planoNecessario").value("PROFISSIONAL"));

        mockMvc.perform(get("/api/receitas").header("Authorization", "Bearer " + sessao.token()))
                .andExpect(status().is(402))
                .andExpect(jsonPath("$.recurso").value("FINANCEIRO"));

        mockMvc.perform(get("/api/despesas").header("Authorization", "Bearer " + sessao.token()))
                .andExpect(status().is(402))
                .andExpect(jsonPath("$.recurso").value("FINANCEIRO"));

        mockMvc.perform(get("/api/comissoes").header("Authorization", "Bearer " + sessao.token()))
                .andExpect(status().is(402))
                .andExpect(jsonPath("$.recurso").value("COMISSOES"));
    }

    @Test
    void planoProfissionalAtivoLiberaDashboardFinanceiroEComissoes() throws Exception {
        Sessao sessao = registrar("profissional-recursos@teste.com");
        ativarPlano(sessao.barbeariaId(), PlanoSaas.PROFISSIONAL);

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + sessao.token()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/receitas").header("Authorization", "Bearer " + sessao.token()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/despesas").header("Authorization", "Bearer " + sessao.token()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/comissoes").header("Authorization", "Bearer " + sessao.token()))
                .andExpect(status().isOk());
    }

    @Test
    void planoPremiumAtivoTambemLiberaDashboardEFinanceiro() throws Exception {
        Sessao sessao = registrar("premium-recursos@teste.com");
        ativarPlano(sessao.barbeariaId(), PlanoSaas.PREMIUM);

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + sessao.token()))
                .andExpect(status().isOk());
    }

    @Test
    void listaDePlanosContinuaAcessivelMesmoComPlanoBasico() throws Exception {
        Sessao sessao = registrar("planos-acessiveis@teste.com");
        ativarPlano(sessao.barbeariaId(), PlanoSaas.BASICO);

        MvcResult resultado = mockMvc.perform(get("/api/assinatura").header("Authorization", "Bearer " + sessao.token()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(resultado.getResponse().getContentAsString()).get("acessoLiberado").asBoolean()).isTrue();
    }
}
