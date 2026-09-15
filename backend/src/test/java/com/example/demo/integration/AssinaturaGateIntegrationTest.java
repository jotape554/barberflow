package com.example.demo.integration;

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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garante que uma barbearia com o trial vencido fica de fato barrada do painel — e que ela
 * nunca fica presa sem conseguir ver a própria tela de assinatura para resolver a situação.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AssinaturaGateIntegrationTest {

    private record Sessao(String token, Long barbeariaId, String slug) {}

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BarbeariaRepository barbeariaRepository;

    private Sessao registrar(String email) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeBarbearia":"Barbearia Teste","nomeAdmin":"Admin","email":"%s","senha":"123456"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString());
        return new Sessao(json.get("token").asText(), json.get("barbeariaId").asLong(), json.get("barbeariaSlug").asText());
    }

    private void expirarTrial(Long barbeariaId, int diasNoPassado) {
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId).orElseThrow();
        barbearia.setDataFimTrial(LocalDate.now().minusDays(diasNoPassado));
        barbeariaRepository.save(barbearia);
    }

    @Test
    void trialAtivoLiberaAcessoAoPainel() throws Exception {
        Sessao sessao = registrar("trial-ativo@teste.com");

        mockMvc.perform(get("/api/clientes").header("Authorization", "Bearer " + sessao.token()))
                .andExpect(status().isOk());
    }

    @Test
    void trialExpiradoBloqueiaPainelComStatus402() throws Exception {
        Sessao sessao = registrar("trial-expirado@teste.com");
        expirarTrial(sessao.barbeariaId(), 1);

        mockMvc.perform(get("/api/clientes").header("Authorization", "Bearer " + sessao.token()))
                .andExpect(status().is(402));
    }

    @Test
    void telaDeAssinaturaNuncaFicaBloqueadaMesmoComTrialExpirado() throws Exception {
        Sessao sessao = registrar("consegue-ver-assinatura@teste.com");
        expirarTrial(sessao.barbeariaId(), 30);

        mockMvc.perform(get("/api/assinatura").header("Authorization", "Bearer " + sessao.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acessoLiberado").value(false));
    }

    @Test
    void statusAtivaLiberaAcessoMesmoDepoisDoTrialExpirar() throws Exception {
        Sessao sessao = registrar("pagou@teste.com");
        Barbearia barbearia = barbeariaRepository.findById(sessao.barbeariaId()).orElseThrow();
        barbearia.setDataFimTrial(LocalDate.now().minusDays(10));
        barbearia.setStatusAssinaturaSaas(StatusAssinaturaSaas.ATIVA);
        barbeariaRepository.save(barbearia);

        mockMvc.perform(get("/api/clientes").header("Authorization", "Bearer " + sessao.token()))
                .andExpect(status().isOk());
    }

    @Test
    void agendaPublicaDoClienteFinalNaoEBloqueadaPeloTrialDaBarbearia() throws Exception {
        Sessao sessao = registrar("agenda-publica@teste.com");
        expirarTrial(sessao.barbeariaId(), 30);

        MvcResult resultado = mockMvc.perform(get("/public/barbearias/{slug}", sessao.slug()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode resposta = objectMapper.readTree(resultado.getResponse().getContentAsString());
        assertThat(resposta.get("nome").asText()).isEqualTo("Barbearia Teste");
    }
}
