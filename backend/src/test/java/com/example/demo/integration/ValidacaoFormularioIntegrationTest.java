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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cliente, Serviço, Profissional e Despesa agora validam os campos antes de salvar — nome
 * vazio, preço/valor negativo e comissão fora de 0-100% devem virar 400 com mensagem clara,
 * nunca ser salvos silenciosamente.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ValidacaoFormularioIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String registrarEObterToken(String email) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeBarbearia":"Barbearia Validacao","nomeAdmin":"Admin","email":"%s","cpf":"%s","senha":"123456"}
                                """.formatted(email, com.example.demo.util.CpfTestFixture.gerar(email))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void clienteComNomeEmBrancoRetorna400() throws Exception {
        String token = registrarEObterToken("validacao-cliente@teste.com");
        mockMvc.perform(post("/api/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"telefone\":\"11999999999\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Informe o nome do cliente."));
    }

    @Test
    void clienteComEmailInvalidoRetorna400() throws Exception {
        String token = registrarEObterToken("validacao-cliente-email@teste.com");
        mockMvc.perform(post("/api/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria\",\"email\":\"nao-e-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Informe um e-mail válido."));
    }

    @Test
    void servicoComPrecoNegativoRetorna400() throws Exception {
        String token = registrarEObterToken("validacao-servico@teste.com");
        mockMvc.perform(post("/api/servicos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Corte\",\"preco\":-10.00,\"duracaoMinutos\":30,\"ativo\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O preço precisa ser maior que zero."));
    }

    @Test
    void servicoComNomeEmBrancoRetorna400() throws Exception {
        String token = registrarEObterToken("validacao-servico-nome@teste.com");
        mockMvc.perform(post("/api/servicos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"preco\":50.00,\"duracaoMinutos\":30,\"ativo\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Informe o nome do serviço."));
    }

    @Test
    void profissionalComComissaoAcimaDeCemPorCentoRetorna400() throws Exception {
        String token = registrarEObterToken("validacao-profissional@teste.com");
        mockMvc.perform(post("/api/profissionais")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Carlos\",\"funcao\":\"Barbeiro\",\"percentualComissao\":150,\"ativo\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O percentual de comissão não pode passar de 100%."));
    }

    @Test
    void profissionalComComissaoNegativaRetorna400() throws Exception {
        String token = registrarEObterToken("validacao-profissional-neg@teste.com");
        mockMvc.perform(post("/api/profissionais")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Carlos\",\"funcao\":\"Barbeiro\",\"percentualComissao\":-5,\"ativo\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O percentual de comissão não pode ser negativo."));
    }

    @Test
    void despesaComValorNegativoRetorna400() throws Exception {
        String token = registrarEObterToken("validacao-despesa@teste.com");
        mockMvc.perform(post("/api/despesas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"Aluguel\",\"valor\":-300.00,\"data\":\"2026-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O valor precisa ser maior que zero."));
    }
}
