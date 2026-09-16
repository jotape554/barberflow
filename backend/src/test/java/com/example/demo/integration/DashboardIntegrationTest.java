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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O dashboard "completo" (visão geral, comparações, financeiro, ranking de profissionais e
 * serviços, resumo de clientes) precisa refletir exatamente o que está no banco — sem
 * inventar nem arredondar errado nenhum número.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class DashboardIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String registrarEObterToken(String email) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeBarbearia":"Barbearia Dashboard","nomeAdmin":"Admin","email":"%s","senha":"123456"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("token").asText();
    }

    private Long criarServico(String token) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/servicos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Corte\",\"preco\":50.00,\"duracaoMinutos\":30,\"ativo\":true}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long criarProfissional(String token) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/profissionais")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Carlos\",\"funcao\":\"Barbeiro\",\"percentualComissao\":40,\"ativo\":true}"))
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
    void dashboardReflexteExatamenteOAtendimentoConcluidoHoje() throws Exception {
        String token = registrarEObterToken("dashboard-completo@teste.com");
        Long servicoId = criarServico(token);
        Long profissionalId = criarProfissional(token);
        Long clienteId = criarCliente(token);

        MvcResult criacao = mockMvc.perform(post("/api/agendamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clienteId":%d,"profissionalId":%d,"servicoId":%d,"data":"%s","horaInicio":"10:00"}
                                """.formatted(clienteId, profissionalId, servicoId, java.time.LocalDate.now())))
                .andExpect(status().isOk())
                .andReturn();
        Long agendamentoId = objectMapper.readTree(criacao.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/agendamentos/{id}/concluir", agendamentoId)
                        .header("Authorization", "Bearer " + token)
                        .param("formaPagamento", "PIX"))
                .andExpect(status().isOk());

        MvcResult dashboardResultado = mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode dashboard = objectMapper.readTree(dashboardResultado.getResponse().getContentAsString());

        JsonNode visaoGeral = dashboard.get("visaoGeral");
        assertThat(new BigDecimal(visaoGeral.get("faturamentoHoje").asText())).isEqualByComparingTo("50.00");
        assertThat(visaoGeral.get("atendimentosHoje").asInt()).isEqualTo(1);
        assertThat(visaoGeral.get("clientesCadastrados").asInt()).isEqualTo(1);
        assertThat(visaoGeral.get("novosClientesMes").asInt()).isEqualTo(1);
        assertThat(visaoGeral.get("clientesRecorrentesMes").asInt()).isEqualTo(0);

        JsonNode comparacoes = dashboard.get("comparacoes");
        assertThat(new BigDecimal(comparacoes.get("faturamentoOntem").asText())).isEqualByComparingTo("0");
        assertThat(comparacoes.get("variacaoHojeOntemPercentual").isNull()).isTrue();

        JsonNode financeiro = dashboard.get("financeiro");
        assertThat(new BigDecimal(financeiro.get("faturamentoPorFormaPagamento").get("PIX").asText())).isEqualByComparingTo("50.00");
        assertThat(new BigDecimal(financeiro.get("comissoesMes").asText())).isEqualByComparingTo("20.00");
        assertThat(new BigDecimal(financeiro.get("despesasMes").asText())).isEqualByComparingTo("0");
        assertThat(new BigDecimal(financeiro.get("lucroLiquidoMes").asText())).isEqualByComparingTo("30.00");
        assertThat(financeiro.get("faturamentoPorDia").isArray()).isTrue();
        assertThat(financeiro.get("faturamentoPorDia")).hasSize(14);

        JsonNode profissionais = dashboard.get("profissionais");
        assertThat(profissionais).hasSize(1);
        assertThat(profissionais.get(0).get("nome").asText()).isEqualTo("Carlos");
        assertThat(profissionais.get(0).get("atendimentos").asInt()).isEqualTo(1);
        assertThat(new BigDecimal(profissionais.get(0).get("faturamento").asText())).isEqualByComparingTo("50.00");
        assertThat(new BigDecimal(profissionais.get(0).get("comissao").asText())).isEqualByComparingTo("20.00");
        assertThat(profissionais.get(0).get("clientesAtendidos").asInt()).isEqualTo(1);

        JsonNode servicos = dashboard.get("servicos");
        assertThat(servicos).hasSize(1);
        assertThat(servicos.get(0).get("nome").asText()).isEqualTo("Corte");
        assertThat(servicos.get(0).get("quantidade").asInt()).isEqualTo(1);

        JsonNode clientesResumo = dashboard.get("clientes");
        assertThat(clientesResumo.get("novos").asInt()).isEqualTo(1);
        assertThat(clientesResumo.get("recorrentes").asInt()).isEqualTo(0);
        assertThat(clientesResumo.get("inativos30Dias").asInt()).isEqualTo(0);
    }

    @Test
    void dashboardDeUmaBarbeariaNuncaMostraNumeroDeOutra() throws Exception {
        String tokenA = registrarEObterToken("dashboard-a@teste.com");
        String tokenB = registrarEObterToken("dashboard-b@teste.com");

        Long servicoId = criarServico(tokenA);
        Long profissionalId = criarProfissional(tokenA);
        Long clienteId = criarCliente(tokenA);

        MvcResult criacao = mockMvc.perform(post("/api/agendamentos")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clienteId":%d,"profissionalId":%d,"servicoId":%d,"data":"%s","horaInicio":"10:00"}
                                """.formatted(clienteId, profissionalId, servicoId, java.time.LocalDate.now())))
                .andExpect(status().isOk())
                .andReturn();
        Long agendamentoId = objectMapper.readTree(criacao.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(patch("/api/agendamentos/{id}/concluir", agendamentoId)
                        .header("Authorization", "Bearer " + tokenA)
                        .param("formaPagamento", "PIX"))
                .andExpect(status().isOk());

        MvcResult dashboardB = mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode dashboard = objectMapper.readTree(dashboardB.getResponse().getContentAsString());
        assertThat(new BigDecimal(dashboard.get("visaoGeral").get("faturamentoHoje").asText())).isEqualByComparingTo("0");
        assertThat(dashboard.get("profissionais")).isEmpty();
    }
}
