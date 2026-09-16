package com.example.demo.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Cobre achados da auditoria de segurança: dados internos da Stripe nunca devem sair em
 * respostas de API, e erros de validação devem virar 400 com mensagem em português — nunca
 * 500 com detalhe técnico interno vazando pro cliente (foi o que quebrou o login em produção).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AuditoriaSegurancaIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String registrarEObterToken(String email) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeBarbearia":"Barbearia Teste","nomeAdmin":"Admin","email":"%s","senha":"123456"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void dadosInternosDaStripeNuncaAparecemEmRespostaDeCliente() throws Exception {
        String token = registrarEObterToken("auditoria-stripe@teste.com");

        MvcResult resultado = mockMvc.perform(post("/api/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria\",\"telefone\":\"11999999999\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode cliente = objectMapper.readTree(resultado.getResponse().getContentAsString());
        JsonNode barbearia = cliente.get("barbearia");
        if (barbearia != null && !barbearia.isNull()) {
            assertThat(barbearia.has("stripeCustomerId")).isFalse();
            assertThat(barbearia.has("stripeSubscriptionId")).isFalse();
        }
    }

    @Test
    void loginComEmailInvalidoRetorna400ComMensagemEmPortuguesNao500() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nao-e-um-email\",\"senha\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Informe um e-mail válido."));
    }

    @Test
    void registroSemNomeDaBarbeariaRetorna400NaoLevantaErroInterno() throws Exception {
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeBarbearia\":\"\",\"nomeAdmin\":\"Admin\",\"email\":\"valido@teste.com\",\"senha\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Informe o nome da barbearia."));
    }
}
