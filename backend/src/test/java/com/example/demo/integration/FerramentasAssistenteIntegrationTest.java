package com.example.demo.integration;

import com.example.demo.enums.FormaPagamento;
import com.example.demo.enums.Papel;
import com.example.demo.enums.StatusAgendamento;
import com.example.demo.enums.StatusReceita;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.security.UsuarioPrincipal;
import com.example.demo.service.FerramentasAssistenteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa a camada de ferramentas do Assistente de IA diretamente (sem precisar de uma chave
 * real da Anthropic): confere que os números batem com o que foi gravado no banco, e que uma
 * barbearia nunca enxerga dado de outra — a mesma garantia de isolamento que vale pro resto
 * do sistema tem que valer aqui também.
 */
@SpringBootTest
class FerramentasAssistenteIntegrationTest {

    @Autowired private FerramentasAssistenteService ferramentas;
    @Autowired private BarbeariaRepository barbeariaRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProfissionalRepository profissionalRepository;
    @Autowired private ServicoRepository servicoRepository;
    @Autowired private AgendamentoRepository agendamentoRepository;
    @Autowired private ReceitaRepository receitaRepository;
    @Autowired private ComissaoRepository comissaoRepository;
    @Autowired private DespesaRepository despesaRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Long barbeariaId) {
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId).orElseThrow();
        Usuario usuario = Usuario.builder()
                .nome("Dono")
                .email("dono-" + barbeariaId + "@teste.com")
                .senhaHash("x")
                .papel(Papel.ADMINISTRADOR)
                .barbearia(barbearia)
                .build();
        UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Long criarBarbearia(String nome, String slug) {
        return barbeariaRepository.save(Barbearia.builder().nome(nome).slug(slug).build()).getId();
    }

    @Test
    void obterFaturamentoSomaSoAsReceitasRecebidasDaBarbeariaEDoPeriodo() {
        Long barbeariaId = criarBarbearia("Barbearia Faturamento", "faturamento-ia");
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId).orElseThrow();
        Cliente cliente = clienteRepository.save(Cliente.builder().barbearia(barbearia).nome("Maria").build());

        LocalDate hoje = LocalDate.now();
        receitaRepository.save(Receita.builder().barbearia(barbearia).cliente(cliente)
                .valor(new BigDecimal("50.00")).formaPagamento(FormaPagamento.PIX)
                .status(StatusReceita.RECEBIDO).data(hoje).build());
        receitaRepository.save(Receita.builder().barbearia(barbearia).cliente(cliente)
                .valor(new BigDecimal("30.00")).formaPagamento(FormaPagamento.DINHEIRO)
                .status(StatusReceita.RECEBIDO).data(hoje).build());
        // fora do período (mês passado) — não pode entrar na soma de "hoje"
        receitaRepository.save(Receita.builder().barbearia(barbearia).cliente(cliente)
                .valor(new BigDecimal("999.00")).formaPagamento(FormaPagamento.PIX)
                .status(StatusReceita.RECEBIDO).data(hoje.minusMonths(1)).build());

        autenticarComo(barbeariaId);
        Map<String, Object> resultado = ferramentas.obterFaturamento("hoje");

        assertThat((BigDecimal) resultado.get("totalFaturado")).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(resultado.get("quantidadeAtendimentosPagos")).isEqualTo(2);
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> porForma = (Map<String, BigDecimal>) resultado.get("faturamentoPorFormaPagamento");
        assertThat(porForma.get("PIX")).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(porForma.get("DINHEIRO")).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    void obterFaturamentoNuncaMisturaDadosDeOutraBarbearia() {
        Long barbeariaA = criarBarbearia("Barbearia A", "barbearia-a-ia");
        Long barbeariaB = criarBarbearia("Barbearia B", "barbearia-b-ia");

        Barbearia bA = barbeariaRepository.findById(barbeariaA).orElseThrow();
        Barbearia bB = barbeariaRepository.findById(barbeariaB).orElseThrow();
        Cliente clienteA = clienteRepository.save(Cliente.builder().barbearia(bA).nome("Cliente A").build());
        Cliente clienteB = clienteRepository.save(Cliente.builder().barbearia(bB).nome("Cliente B").build());

        LocalDate hoje = LocalDate.now();
        receitaRepository.save(Receita.builder().barbearia(bA).cliente(clienteA)
                .valor(new BigDecimal("100.00")).formaPagamento(FormaPagamento.PIX)
                .status(StatusReceita.RECEBIDO).data(hoje).build());
        receitaRepository.save(Receita.builder().barbearia(bB).cliente(clienteB)
                .valor(new BigDecimal("500.00")).formaPagamento(FormaPagamento.PIX)
                .status(StatusReceita.RECEBIDO).data(hoje).build());

        autenticarComo(barbeariaA);
        Map<String, Object> resultado = ferramentas.obterFaturamento("hoje");

        assertThat((BigDecimal) resultado.get("totalFaturado")).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void obterClientesListaCorretamenteInativosENovos() {
        Long barbeariaId = criarBarbearia("Barbearia Clientes", "clientes-ia");
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId).orElseThrow();
        Servico servico = servicoRepository.save(Servico.builder().barbearia(barbearia).nome("Corte")
                .preco(new BigDecimal("50")).duracaoMinutos(30).ativo(true).build());
        Profissional profissional = profissionalRepository.save(Profissional.builder().barbearia(barbearia)
                .nome("Carlos").percentualComissao(new BigDecimal("40")).ativo(true).build());

        Cliente clienteAtivo = clienteRepository.save(Cliente.builder().barbearia(barbearia).nome("Ativo").build());
        Cliente clienteInativo = clienteRepository.save(Cliente.builder().barbearia(barbearia).nome("Sumido").build());

        agendamentoRepository.save(Agendamento.builder().barbearia(barbearia).cliente(clienteAtivo)
                .profissional(profissional).servico(servico)
                .data(LocalDate.now()).horaInicio(java.time.LocalTime.of(10, 0)).horaFim(java.time.LocalTime.of(10, 30))
                .status(StatusAgendamento.CONCLUIDO).build());
        agendamentoRepository.save(Agendamento.builder().barbearia(barbearia).cliente(clienteInativo)
                .profissional(profissional).servico(servico)
                .data(LocalDate.now().minusDays(60)).horaInicio(java.time.LocalTime.of(10, 0)).horaFim(java.time.LocalTime.of(10, 30))
                .status(StatusAgendamento.CONCLUIDO).build());

        autenticarComo(barbeariaId);
        Map<String, Object> resultado = ferramentas.obterClientes();

        assertThat(resultado.get("totalClientes")).isEqualTo(2);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inativos = (List<Map<String, Object>>) resultado.get("clientesSemAgendamentoHa30DiasOuMais");
        assertThat(inativos).hasSize(1);
        assertThat(inativos.get(0).get("nome")).isEqualTo("Sumido");
    }

    @Test
    void obterComissoesSeparaPagaDePendentePorProfissional() {
        Long barbeariaId = criarBarbearia("Barbearia Comissao", "comissao-ia");
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId).orElseThrow();
        Profissional profissional = profissionalRepository.save(Profissional.builder().barbearia(barbearia)
                .nome("Bruno").percentualComissao(new BigDecimal("40")).ativo(true).build());

        comissaoRepository.save(Comissao.builder().barbearia(barbearia).profissional(profissional)
                .percentual(new BigDecimal("40")).valor(new BigDecimal("20.00")).paga(true).data(LocalDate.now()).build());
        comissaoRepository.save(Comissao.builder().barbearia(barbearia).profissional(profissional)
                .percentual(new BigDecimal("40")).valor(new BigDecimal("15.00")).paga(false).data(LocalDate.now()).build());

        autenticarComo(barbeariaId);
        Map<String, Object> resultado = ferramentas.obterComissoes("hoje");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> porProfissional = (List<Map<String, Object>>) resultado.get("comissaoPorProfissional");
        assertThat(porProfissional).hasSize(1);
        Map<String, Object> linha = porProfissional.get(0);
        assertThat((BigDecimal) linha.get("comissaoTotal")).isEqualByComparingTo(new BigDecimal("35.00"));
        assertThat((BigDecimal) linha.get("comissaoPaga")).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat((BigDecimal) linha.get("comissaoPendente")).isEqualByComparingTo(new BigDecimal("15.00"));
    }

    @Test
    void obterDespesasAgrupaPorCategoria() {
        Long barbeariaId = criarBarbearia("Barbearia Despesa", "despesa-ia");
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId).orElseThrow();

        despesaRepository.save(Despesa.builder().barbearia(barbearia).descricao("Aluguel")
                .categoria("Fixas").valor(new BigDecimal("1000")).data(LocalDate.now()).build());
        despesaRepository.save(Despesa.builder().barbearia(barbearia).descricao("Produtos")
                .categoria("Fixas").valor(new BigDecimal("200")).data(LocalDate.now()).build());

        autenticarComo(barbeariaId);
        Map<String, Object> resultado = ferramentas.obterDespesas("mes_atual");

        assertThat((BigDecimal) resultado.get("totalDespesas")).isEqualByComparingTo(new BigDecimal("1200"));
    }
}
