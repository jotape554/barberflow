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
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo de agendamento pela página pública (sem login), incluindo o limite de tentativas por
 * IP que protege contra um script lotando a agenda com horários falsos.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AgendamentoPublicoIntegrationTest {

    private record Setup(String slug, Long servicoId, Long profissionalId) {}

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private Setup registrarBarbeariaComServicoEProfissional(String email) throws Exception {
        MvcResult registro = mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeBarbearia":"Barbearia Publica","nomeAdmin":"Admin","email":"%s","cpf":"%s","senha":"123456"}
                                """.formatted(email, com.example.demo.util.CpfTestFixture.gerar(email))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(registro.getResponse().getContentAsString());
        String token = json.get("token").asText();
        String slug = json.get("barbeariaSlug").asText();

        MvcResult servico = mockMvc.perform(post("/api/servicos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Corte\",\"preco\":50.00,\"duracaoMinutos\":30,\"ativo\":true}"))
                .andExpect(status().isOk())
                .andReturn();
        Long servicoId = objectMapper.readTree(servico.getResponse().getContentAsString()).get("id").asLong();

        MvcResult profissional = mockMvc.perform(post("/api/profissionais")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Carlos\",\"funcao\":\"Barbeiro\",\"percentualComissao\":40,\"ativo\":true}"))
                .andExpect(status().isOk())
                .andReturn();
        Long profissionalId = objectMapper.readTree(profissional.getResponse().getContentAsString()).get("id").asLong();

        return new Setup(slug, servicoId, profissionalId);
    }

    private String corpoAgendamento(Setup s, String hora) {
        LocalDate amanha = LocalDate.now().plusDays(1);
        return """
                {"nomeCliente":"Cliente Teste","telefoneCliente":"11999999999","profissionalId":%d,"servicoId":%d,"data":"%s","horaInicio":"%s"}
                """.formatted(s.profissionalId(), s.servicoId(), amanha, hora);
    }

    @Test
    void clienteFinalConsegueAgendarPelaPaginaPublicaSemLogin() throws Exception {
        Setup s = registrarBarbeariaComServicoEProfissional("publico-ok@teste.com");

        // IP fictício exclusivo deste teste — o limitador é um contador global por IP na JVM,
        // então sem isso um teste de limite rodando antes contaminaria este aqui.
        mockMvc.perform(post("/public/barbearias/{slug}/agendamentos", s.slug())
                        .header("X-Forwarded-For", "10.0.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAgendamento(s, "09:00")))
                .andExpect(status().isOk());
    }

    @Test
    void depoisDeMuitasTentativasNoMesmoMinutoORequestSeguinteVirA429() throws Exception {
        Setup s = registrarBarbeariaComServicoEProfissional("publico-limite@teste.com");

        int totalTentativas = 12;
        int qtd429 = 0;
        for (int i = 0; i < totalTentativas; i++) {
            LocalTime hora = LocalTime.of(9, 0).plusMinutes(i * 30L);
            MvcResult resultado = mockMvc.perform(post("/public/barbearias/{slug}/agendamentos", s.slug())
                            .header("X-Forwarded-For", "10.0.0.2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoAgendamento(s, hora.toString())))
                    .andReturn();
            if (resultado.getResponse().getStatus() == 429) {
                qtd429++;
            }
        }

        assertThat(qtd429).isGreaterThan(0);
    }

    @Test
    void limiteNaoAfetaConsultasDeLeituraDaPaginaPublica() throws Exception {
        Setup s = registrarBarbeariaComServicoEProfissional("publico-leitura@teste.com");

        // várias leituras seguidas nunca devem ser bloqueadas — só a criação de agendamento conta.
        for (int i = 0; i < 15; i++) {
            mockMvc.perform(get("/public/barbearias/{slug}/servicos", s.slug()))
                    .andExpect(status().isOk());
        }
    }
}
