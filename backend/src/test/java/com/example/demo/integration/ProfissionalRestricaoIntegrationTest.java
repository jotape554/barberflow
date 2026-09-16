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
 * Um profissional nunca pode enxergar (ou mexer n)a agenda e na comissão de outro profissional
 * da mesma barbearia — só na própria. O administrador continua vendo tudo normalmente.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ProfissionalRestricaoIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String registrarBarbeariaEObterToken(String email) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nomeBarbearia":"Barbearia Teste","nomeAdmin":"Admin","email":"%s","cpf":"%s","senha":"123456"}
                                """.formatted(email, com.example.demo.util.CpfTestFixture.gerar(email))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("token").asText();
    }

    private Long criarServico(String token) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/servicos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Corte\",\"descricao\":\"Corte simples\",\"preco\":50.00,\"duracaoMinutos\":30,\"ativo\":true}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long criarProfissional(String token, String nome) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/profissionais")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"%s","funcao":"Barbeiro","percentualComissao":40,"ativo":true}
                                """.formatted(nome)))
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

    private Long criarAgendamento(String token, Long clienteId, Long profissionalId, Long servicoId, String hora) throws Exception {
        LocalDate amanha = LocalDate.now().plusDays(1);
        MvcResult resultado = mockMvc.perform(post("/api/agendamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clienteId":%d,"profissionalId":%d,"servicoId":%d,"data":"%s","horaInicio":"%s"}
                                """.formatted(clienteId, profissionalId, servicoId, amanha, hora)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private String criarAcessoELogar(String tokenAdmin, Long profissionalId, String email) throws Exception {
        mockMvc.perform(post("/api/profissionais/{id}/acesso", profissionalId)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"123456\"}".formatted(email)))
                .andExpect(status().isNoContent());

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"123456\"}".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("PROFISSIONAL"))
                .andExpect(jsonPath("$.barbeariaId").isNotEmpty())
                .andExpect(jsonPath("$.barbeariaSlug").isNotEmpty())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void profissionalSoVeAPropriaAgendaNaoADeOutroProfissional() throws Exception {
        String tokenAdmin = registrarBarbeariaEObterToken("dono-agenda@teste.com");
        Long servicoId = criarServico(tokenAdmin);
        Long carlosId = criarProfissional(tokenAdmin, "Carlos");
        Long brunoId = criarProfissional(tokenAdmin, "Bruno");
        Long clienteId = criarCliente(tokenAdmin, "Maria");

        Long agendamentoCarlos = criarAgendamento(tokenAdmin, clienteId, carlosId, servicoId, "10:00");
        Long agendamentoBruno = criarAgendamento(tokenAdmin, clienteId, brunoId, servicoId, "14:00");

        String tokenCarlos = criarAcessoELogar(tokenAdmin, carlosId, "carlos@teste.com");

        MvcResult listaCarlos = mockMvc.perform(get("/api/agendamentos")
                        .header("Authorization", "Bearer " + tokenCarlos))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode agendamentosDeCarlos = objectMapper.readTree(listaCarlos.getResponse().getContentAsString()).get("content");
        assertThat(agendamentosDeCarlos).hasSize(1);
        assertThat(agendamentosDeCarlos.get(0).get("id").asLong()).isEqualTo(agendamentoCarlos);

        // não consegue nem buscar o agendamento do Bruno diretamente pelo id
        mockMvc.perform(get("/api/agendamentos/{id}", agendamentoBruno)
                        .header("Authorization", "Bearer " + tokenCarlos))
                .andExpect(status().isNotFound());

        // admin continua vendo os dois
        MvcResult listaAdmin = mockMvc.perform(get("/api/agendamentos")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(listaAdmin.getResponse().getContentAsString()).get("content")).hasSize(2);
    }

    @Test
    void profissionalNaoConsegueCriarAgendamentoEmNomeDeOutroProfissional() throws Exception {
        String tokenAdmin = registrarBarbeariaEObterToken("dono-criar@teste.com");
        Long servicoId = criarServico(tokenAdmin);
        Long carlosId = criarProfissional(tokenAdmin, "Carlos");
        Long brunoId = criarProfissional(tokenAdmin, "Bruno");
        Long clienteId = criarCliente(tokenAdmin, "Maria");

        String tokenCarlos = criarAcessoELogar(tokenAdmin, carlosId, "carlos2@teste.com");

        LocalDate amanha = LocalDate.now().plusDays(1);
        mockMvc.perform(post("/api/agendamentos")
                        .header("Authorization", "Bearer " + tokenCarlos)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clienteId":%d,"profissionalId":%d,"servicoId":%d,"data":"%s","horaInicio":"09:00"}
                                """.formatted(clienteId, brunoId, servicoId, amanha)))
                .andExpect(status().isOk())
                // mesmo pedindo o id do Bruno, o agendamento nasce vinculado ao próprio Carlos
                .andExpect(jsonPath("$.profissional.id").value(carlosId));
    }

    @Test
    void profissionalSoVeAPropriaComissao() throws Exception {
        String tokenAdmin = registrarBarbeariaEObterToken("dono-comissao@teste.com");
        Long servicoId = criarServico(tokenAdmin);
        Long carlosId = criarProfissional(tokenAdmin, "Carlos");
        Long brunoId = criarProfissional(tokenAdmin, "Bruno");
        Long clienteId = criarCliente(tokenAdmin, "Maria");

        Long agendamentoCarlos = criarAgendamento(tokenAdmin, clienteId, carlosId, servicoId, "10:00");
        Long agendamentoBruno = criarAgendamento(tokenAdmin, clienteId, brunoId, servicoId, "14:00");

        mockMvc.perform(patch("/api/agendamentos/{id}/concluir", agendamentoCarlos)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .param("formaPagamento", "PIX"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/agendamentos/{id}/concluir", agendamentoBruno)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .param("formaPagamento", "PIX"))
                .andExpect(status().isOk());

        String tokenCarlos = criarAcessoELogar(tokenAdmin, carlosId, "carlos3@teste.com");

        MvcResult comissoesCarlos = mockMvc.perform(get("/api/comissoes")
                        .header("Authorization", "Bearer " + tokenCarlos))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode comissoes = objectMapper.readTree(comissoesCarlos.getResponse().getContentAsString());
        assertThat(comissoes).hasSize(1);
        assertThat(comissoes.get(0).get("profissional").get("id").asLong()).isEqualTo(carlosId);
    }

    @Test
    void profissionalNaoAcessaDashboardNemFinanceiroDaBarbeariaNemEditaCadastros() throws Exception {
        String tokenAdmin = registrarBarbeariaEObterToken("dono-restricoes@teste.com");
        Long servicoId = criarServico(tokenAdmin);
        Long carlosId = criarProfissional(tokenAdmin, "Carlos");
        Long brunoId = criarProfissional(tokenAdmin, "Bruno");

        String tokenCarlos = criarAcessoELogar(tokenAdmin, carlosId, "carlos6@teste.com");

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + tokenCarlos))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/receitas").header("Authorization", "Bearer " + tokenCarlos))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/despesas").header("Authorization", "Bearer " + tokenCarlos))
                .andExpect(status().isForbidden());

        // não pode editar o cadastro de um colega nem do catálogo de serviços
        mockMvc.perform(put("/api/profissionais/{id}", brunoId)
                        .header("Authorization", "Bearer " + tokenCarlos)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Bruno Hackeado\",\"percentualComissao\":99,\"ativo\":true}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/servicos/{id}", servicoId)
                        .header("Authorization", "Bearer " + tokenCarlos)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Corte\",\"preco\":999.00,\"duracaoMinutos\":30,\"ativo\":true}"))
                .andExpect(status().isForbidden());

        // mas continua conseguindo ver clientes/serviços/profissionais pra montar a própria agenda
        mockMvc.perform(get("/api/servicos").header("Authorization", "Bearer " + tokenCarlos))
                .andExpect(status().isOk());
    }

    @Test
    void profissionalNaoVeTelefoneEmailNemComissaoDeColegaMasVeOsProprios() throws Exception {
        String tokenAdmin = registrarBarbeariaEObterToken("dono-dados-sensiveis@teste.com");
        Long carlosId = criarProfissional(tokenAdmin, "Carlos");
        Long brunoId = criarProfissional(tokenAdmin, "Bruno");

        mockMvc.perform(put("/api/profissionais/{id}", carlosId)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Carlos\",\"telefone\":\"11911112222\",\"email\":\"carlos@teste.com\",\"percentualComissao\":40,\"ativo\":true}"))
                .andExpect(status().isOk());

        String tokenCarlos = criarAcessoELogar(tokenAdmin, carlosId, "carlos7@teste.com");

        MvcResult resultado = mockMvc.perform(get("/api/profissionais").header("Authorization", "Bearer " + tokenCarlos))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode lista = objectMapper.readTree(resultado.getResponse().getContentAsString());

        JsonNode dadosCarlos = null;
        JsonNode dadosBruno = null;
        for (JsonNode item : lista) {
            if (item.get("id").asLong() == carlosId) dadosCarlos = item;
            if (item.get("id").asLong() == brunoId) dadosBruno = item;
        }

        assertThat(dadosCarlos.get("telefone").asText()).isEqualTo("11911112222");
        assertThat(dadosCarlos.get("email").asText()).isEqualTo("carlos@teste.com");
        assertThat(dadosCarlos.get("percentualComissao").asDouble()).isEqualTo(40.0);

        assertThat(dadosBruno.get("telefone").isNull()).isTrue();
        assertThat(dadosBruno.get("email").isNull()).isTrue();
        assertThat(dadosBruno.get("percentualComissao").isNull()).isTrue();
        assertThat(dadosBruno.get("nome").asText()).isEqualTo("Bruno");

        // admin continua vendo tudo de todo mundo
        MvcResult resultadoAdmin = mockMvc.perform(get("/api/profissionais").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode listaAdmin = objectMapper.readTree(resultadoAdmin.getResponse().getContentAsString());
        for (JsonNode item : listaAdmin) {
            if (item.get("id").asLong() == brunoId) {
                assertThat(item.get("percentualComissao").isNull()).isFalse();
            }
        }
    }

    @Test
    void naoDeixaCriarDoisAcessosParaOMesmoProfissional() throws Exception {
        String tokenAdmin = registrarBarbeariaEObterToken("dono-duplo@teste.com");
        Long carlosId = criarProfissional(tokenAdmin, "Carlos");

        mockMvc.perform(post("/api/profissionais/{id}/acesso", carlosId)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"carlos4@teste.com\",\"senha\":\"123456\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/profissionais/{id}/acesso", carlosId)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"carlos5@teste.com\",\"senha\":\"123456\"}"))
                .andExpect(status().isBadRequest());
    }
}
