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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O plano contratado limita quantos profissionais a barbearia pode cadastrar (Básico = 1,
 * Profissional = 5, Premium = ilimitado). Durante o período de teste grátis esse limite não
 * vale — só passa a valer quando a assinatura está de fato ATIVA (paga).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class LimitePlanoIntegrationTest {

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

    private ResultActionsHelper criarProfissional(String token, String nome) throws Exception {
        return new ResultActionsHelper(mockMvc.perform(post("/api/profissionais")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"%s","funcao":"Barbeiro","percentualComissao":40,"ativo":true}
                        """.formatted(nome))));
    }

    private record ResultActionsHelper(org.springframework.test.web.servlet.ResultActions acoes) {
        void esperaStatus(int esperado) throws Exception {
            acoes.andExpect(status().is(esperado));
        }
    }

    @Test
    void duranteOTrialNaoHaLimiteDeProfissionais() throws Exception {
        Sessao sessao = registrar("trial-sem-limite@teste.com");

        criarProfissional(sessao.token(), "Carlos").esperaStatus(200);
        criarProfissional(sessao.token(), "Bruno").esperaStatus(200);
        criarProfissional(sessao.token(), "Diego").esperaStatus(200);
    }

    @Test
    void planoBasicoAtivoPermiteSoUmProfissional() throws Exception {
        Sessao sessao = registrar("basico-limite@teste.com");
        ativarPlano(sessao.barbeariaId(), PlanoSaas.BASICO);

        criarProfissional(sessao.token(), "Carlos").esperaStatus(200);
        criarProfissional(sessao.token(), "Bruno").esperaStatus(400);
    }

    @Test
    void planoProfissionalAtivoPermiteAteCincoProfissionais() throws Exception {
        Sessao sessao = registrar("plano-profissional-limite@teste.com");
        ativarPlano(sessao.barbeariaId(), PlanoSaas.PROFISSIONAL);

        for (int i = 1; i <= 5; i++) {
            criarProfissional(sessao.token(), "Barbeiro " + i).esperaStatus(200);
        }
        criarProfissional(sessao.token(), "Barbeiro 6").esperaStatus(400);
    }

    @Test
    void planoPremiumAtivoNaoTemLimite() throws Exception {
        Sessao sessao = registrar("premium-sem-limite@teste.com");
        ativarPlano(sessao.barbeariaId(), PlanoSaas.PREMIUM);

        for (int i = 1; i <= 8; i++) {
            criarProfissional(sessao.token(), "Barbeiro " + i).esperaStatus(200);
        }
    }
}
