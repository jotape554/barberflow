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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O relatório precisa filtrar corretamente por período/profissional/serviço/forma de
 * pagamento, e nunca misturar dados de outra barbearia.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class RelatorioIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String registrarEObterToken(String email) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeBarbearia":"Barbearia Relatorio","nomeAdmin":"Admin","email":"%s","senha":"123456"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("token").asText();
    }

    private Long criarServico(String token, String nome) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/servicos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"%s\",\"preco\":50.00,\"duracaoMinutos\":30,\"ativo\":true}".formatted(nome)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long criarProfissional(String token, String nome) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/profissionais")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"%s\",\"funcao\":\"Barbeiro\",\"percentualComissao\":40,\"ativo\":true}".formatted(nome)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long criarCliente(String token, String nome) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"%s\",\"telefone\":\"11999999999\"}".formatted(nome)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long criarEConcluirAgendamento(String token, Long clienteId, Long profissionalId, Long servicoId,
                                            String hora, String formaPagamento) throws Exception {
        LocalDate hoje = LocalDate.now();
        MvcResult criacao = mockMvc.perform(post("/api/agendamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clienteId":%d,"profissionalId":%d,"servicoId":%d,"data":"%s","horaInicio":"%s"}
                                """.formatted(clienteId, profissionalId, servicoId, hoje, hora)))
                .andExpect(status().isOk())
                .andReturn();
        Long agendamentoId = objectMapper.readTree(criacao.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(patch("/api/agendamentos/{id}/concluir", agendamentoId)
                        .header("Authorization", "Bearer " + token)
                        .param("formaPagamento", formaPagamento))
                .andExpect(status().isOk());
        return agendamentoId;
    }

    @Test
    void relatorioFiltraPorProfissionalServicoEFormaDePagamento() throws Exception {
        String token = registrarEObterToken("relatorio-filtros@teste.com");
        Long servicoCorte = criarServico(token, "Corte");
        Long servicoBarba = criarServico(token, "Barba");
        Long carlos = criarProfissional(token, "Carlos");
        Long bruno = criarProfissional(token, "Bruno");
        Long cliente = criarCliente(token, "Maria");

        criarEConcluirAgendamento(token, cliente, carlos, servicoCorte, "09:00", "PIX");
        criarEConcluirAgendamento(token, cliente, bruno, servicoBarba, "10:00", "DINHEIRO");

        LocalDate hoje = LocalDate.now();

        // Sem filtro: soma os dois
        MvcResult semFiltro = mockMvc.perform(get("/api/relatorios")
                        .header("Authorization", "Bearer " + token)
                        .param("dataInicio", hoje.toString())
                        .param("dataFim", hoje.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode relatorioGeral = objectMapper.readTree(semFiltro.getResponse().getContentAsString());
        assertThat(new BigDecimal(relatorioGeral.get("faturamentoBruto").asText())).isEqualByComparingTo("100.00");

        // Filtrando só pelo Carlos
        MvcResult porProfissional = mockMvc.perform(get("/api/relatorios")
                        .header("Authorization", "Bearer " + token)
                        .param("dataInicio", hoje.toString())
                        .param("dataFim", hoje.toString())
                        .param("profissionalId", carlos.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode relatorioCarlos = objectMapper.readTree(porProfissional.getResponse().getContentAsString());
        assertThat(new BigDecimal(relatorioCarlos.get("faturamentoBruto").asText())).isEqualByComparingTo("50.00");
        assertThat(relatorioCarlos.get("receitas")).hasSize(1);

        // Filtrando só por forma de pagamento DINHEIRO
        MvcResult porForma = mockMvc.perform(get("/api/relatorios")
                        .header("Authorization", "Bearer " + token)
                        .param("dataInicio", hoje.toString())
                        .param("dataFim", hoje.toString())
                        .param("formaPagamento", "DINHEIRO"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode relatorioDinheiro = objectMapper.readTree(porForma.getResponse().getContentAsString());
        assertThat(new BigDecimal(relatorioDinheiro.get("faturamentoBruto").asText())).isEqualByComparingTo("50.00");
    }

    @Test
    void relatorioNuncaMisturaDadosDeOutraBarbearia() throws Exception {
        String tokenA = registrarEObterToken("relatorio-a@teste.com");
        String tokenB = registrarEObterToken("relatorio-b@teste.com");

        Long servico = criarServico(tokenA, "Corte");
        Long profissional = criarProfissional(tokenA, "Carlos");
        Long cliente = criarCliente(tokenA, "Maria");
        criarEConcluirAgendamento(tokenA, cliente, profissional, servico, "09:00", "PIX");

        LocalDate hoje = LocalDate.now();
        MvcResult resultado = mockMvc.perform(get("/api/relatorios")
                        .header("Authorization", "Bearer " + tokenB)
                        .param("dataInicio", hoje.minusDays(30).toString())
                        .param("dataFim", hoje.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode relatorio = objectMapper.readTree(resultado.getResponse().getContentAsString());
        assertThat(new BigDecimal(relatorio.get("faturamentoBruto").asText())).isEqualByComparingTo("0");
        assertThat(relatorio.get("receitas")).isEmpty();
    }

    @Test
    void dataFimAntesDaDataInicioRetorna400() throws Exception {
        String token = registrarEObterToken("relatorio-datas@teste.com");
        LocalDate hoje = LocalDate.now();

        mockMvc.perform(get("/api/relatorios")
                        .header("Authorization", "Bearer " + token)
                        .param("dataInicio", hoje.toString())
                        .param("dataFim", hoje.minusDays(5).toString()))
                .andExpect(status().isBadRequest());
    }
}
