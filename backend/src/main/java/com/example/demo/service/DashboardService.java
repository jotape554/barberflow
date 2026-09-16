package com.example.demo.service;

import com.example.demo.dto.DashboardResponse;
import com.example.demo.enums.FormaPagamento;
import com.example.demo.enums.StatusAgendamento;
import com.example.demo.enums.StatusReceita;
import com.example.demo.model.Agendamento;
import com.example.demo.model.Cliente;
import com.example.demo.model.Comissao;
import com.example.demo.model.Despesa;
import com.example.demo.model.Receita;
import com.example.demo.repository.AgendamentoRepository;
import com.example.demo.repository.ClienteRepository;
import com.example.demo.repository.ComissaoRepository;
import com.example.demo.repository.DespesaRepository;
import com.example.demo.repository.ReceitaRepository;
import com.example.demo.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Todos os números aqui vêm direto do banco. Sem dado fictício: se não houver movimento no
 * período, o valor retornado é zero e cabe ao frontend mostrar o estado vazio.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int TOP_N = 5;
    private static final int DIAS_GRAFICO = 14;

    private final ReceitaRepository receitaRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final ClienteRepository clienteRepository;
    private final DespesaRepository despesaRepository;
    private final ComissaoRepository comissaoRepository;

    public DashboardResponse gerar() {
        Long barbeariaId = SecurityUtils.barbeariaAtualId();
        LocalDate hoje = LocalDate.now();
        LocalDate ontem = hoje.minusDays(1);
        LocalDate inicioSemanaAtual = hoje.with(DayOfWeek.MONDAY);
        LocalDate inicioSemanaAnterior = inicioSemanaAtual.minusWeeks(1);
        LocalDate fimSemanaAnterior = inicioSemanaAtual.minusDays(1);
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth());
        LocalDate mesPassadoRef = hoje.minusMonths(1);
        LocalDate inicioMesPassado = mesPassadoRef.withDayOfMonth(1);
        LocalDate fimMesPassado = mesPassadoRef.withDayOfMonth(mesPassadoRef.lengthOfMonth());
        LocalDate inicioGrafico = hoje.minusDays(DIAS_GRAFICO - 1);

        List<Receita> receitasHoje = receitasRecebidas(barbeariaId, hoje, hoje);
        List<Receita> receitasOntem = receitasRecebidas(barbeariaId, ontem, ontem);
        List<Receita> receitasSemanaAtual = receitasRecebidas(barbeariaId, inicioSemanaAtual, hoje);
        List<Receita> receitasSemanaAnterior = receitasRecebidas(barbeariaId, inicioSemanaAnterior, fimSemanaAnterior);
        List<Receita> receitasMesAtual = receitasRecebidas(barbeariaId, inicioMes, fimMes);
        List<Receita> receitasMesAnterior = receitasRecebidas(barbeariaId, inicioMesPassado, fimMesPassado);
        List<Receita> receitasGrafico = receitasRecebidas(barbeariaId, inicioGrafico, hoje);

        BigDecimal faturamentoHoje = soma(receitasHoje);
        BigDecimal faturamentoOntem = soma(receitasOntem);
        BigDecimal faturamentoSemanaAtual = soma(receitasSemanaAtual);
        BigDecimal faturamentoSemanaAnterior = soma(receitasSemanaAnterior);
        BigDecimal faturamentoMesAtual = soma(receitasMesAtual);
        BigDecimal faturamentoMesAnterior = soma(receitasMesAnterior);

        List<Agendamento> todosAgendamentos = agendamentoRepository.findAllByBarbeariaId(barbeariaId);
        long atendimentosHoje = contarNoPeriodo(todosAgendamentos, hoje, hoje);
        long atendimentosSemana = contarNoPeriodo(todosAgendamentos, inicioSemanaAtual, hoje);
        long atendimentosMes = contarNoPeriodo(todosAgendamentos, inicioMes, fimMes);

        List<Cliente> clientes = clienteRepository.findAllByBarbeariaId(barbeariaId);
        long clientesCadastrados = clientes.size();

        long qtdReceitasMes = receitasMesAtual.size();
        BigDecimal ticketMedioMes = qtdReceitasMes == 0
                ? BigDecimal.ZERO
                : faturamentoMesAtual.divide(BigDecimal.valueOf(qtdReceitasMes), 2, RoundingMode.HALF_UP);

        LocalDateTime inicioMesDateTime = inicioMes.atStartOfDay();
        long novosClientesMes = clientes.stream()
                .filter(c -> c.getDataCadastro() != null && !c.getDataCadastro().isBefore(inicioMesDateTime))
                .count();

        Map<Long, List<LocalDate>> historicoConcluidoPorCliente = new HashMap<>();
        for (Agendamento a : todosAgendamentos) {
            if (a.getStatus() != StatusAgendamento.CONCLUIDO) continue;
            historicoConcluidoPorCliente.computeIfAbsent(a.getCliente().getId(), k -> new ArrayList<>()).add(a.getData());
        }
        long clientesRecorrentesMes = historicoConcluidoPorCliente.values().stream()
                .filter(datas -> datas.stream().anyMatch(d -> !d.isBefore(inicioMes) && !d.isAfter(fimMes)))
                .filter(datas -> datas.stream().anyMatch(d -> d.isBefore(inicioMes)))
                .count();

        DashboardResponse.VisaoGeral visaoGeral = new DashboardResponse.VisaoGeral(
                faturamentoHoje, faturamentoSemanaAtual, faturamentoMesAtual,
                atendimentosHoje, atendimentosSemana, atendimentosMes,
                clientesCadastrados, ticketMedioMes, novosClientesMes, clientesRecorrentesMes);

        DashboardResponse.Comparacoes comparacoes = new DashboardResponse.Comparacoes(
                faturamentoHoje, faturamentoOntem, variacaoPercentual(faturamentoHoje, faturamentoOntem),
                faturamentoSemanaAtual, faturamentoSemanaAnterior, variacaoPercentual(faturamentoSemanaAtual, faturamentoSemanaAnterior),
                faturamentoMesAtual, faturamentoMesAnterior, variacaoPercentual(faturamentoMesAtual, faturamentoMesAnterior));

        DashboardResponse.Financeiro financeiro = montarFinanceiro(
                barbeariaId, inicioGrafico, hoje, receitasGrafico, receitasMesAtual, inicioMes, fimMes, faturamentoMesAtual);

        List<DashboardResponse.ProfissionalDesempenho> profissionais =
                montarRankingProfissionais(barbeariaId, todosAgendamentos, receitasMesAtual, inicioMes, fimMes);

        List<DashboardResponse.ServicoDesempenho> servicos =
                montarRankingServicos(todosAgendamentos, receitasMesAtual, inicioMes, fimMes);

        DashboardResponse.ClientesResumo clientesResumo =
                montarResumoClientes(clientes, todosAgendamentos, hoje, novosClientesMes, clientesRecorrentesMes);

        return new DashboardResponse(visaoGeral, comparacoes, financeiro, profissionais, servicos, clientesResumo);
    }

    private DashboardResponse.Financeiro montarFinanceiro(Long barbeariaId, LocalDate inicioGrafico, LocalDate hoje,
                                                            List<Receita> receitasGrafico, List<Receita> receitasMesAtual,
                                                            LocalDate inicioMes, LocalDate fimMes, BigDecimal faturamentoMesAtual) {
        Map<LocalDate, BigDecimal> somaPorDia = new HashMap<>();
        for (Receita r : receitasGrafico) {
            somaPorDia.merge(r.getData(), r.getValor(), BigDecimal::add);
        }
        List<DashboardResponse.PontoFaturamentoDia> faturamentoPorDia = new ArrayList<>();
        for (LocalDate dia = inicioGrafico; !dia.isAfter(hoje); dia = dia.plusDays(1)) {
            faturamentoPorDia.add(new DashboardResponse.PontoFaturamentoDia(
                    dia.toString(), somaPorDia.getOrDefault(dia, BigDecimal.ZERO)));
        }

        Map<String, BigDecimal> faturamentoPorForma = new LinkedHashMap<>();
        for (FormaPagamento forma : FormaPagamento.values()) {
            BigDecimal valor = receitasMesAtual.stream()
                    .filter(r -> r.getFormaPagamento() == forma)
                    .map(Receita::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (valor.compareTo(BigDecimal.ZERO) > 0) {
                faturamentoPorForma.put(forma.name(), valor);
            }
        }

        BigDecimal despesasMes = despesaRepository.findAllByBarbeariaIdAndDataBetween(barbeariaId, inicioMes, fimMes)
                .stream().map(Despesa::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal comissoesMes = comissaoRepository.findAllByBarbeariaId(barbeariaId).stream()
                .filter(c -> !c.getData().isBefore(inicioMes) && !c.getData().isAfter(fimMes))
                .map(Comissao::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal lucroLiquidoMes = faturamentoMesAtual.subtract(despesasMes).subtract(comissoesMes);

        return new DashboardResponse.Financeiro(faturamentoPorDia, faturamentoPorForma, despesasMes, comissoesMes, lucroLiquidoMes);
    }

    private List<DashboardResponse.ProfissionalDesempenho> montarRankingProfissionais(
            Long barbeariaId, List<Agendamento> todosAgendamentos, List<Receita> receitasMesAtual,
            LocalDate inicioMes, LocalDate fimMes) {

        Map<String, Long> atendimentosPorProfissional = new LinkedHashMap<>();
        Map<String, Set<Long>> clientesPorProfissional = new HashMap<>();
        for (Agendamento a : todosAgendamentos) {
            if (a.getStatus() != StatusAgendamento.CONCLUIDO) continue;
            if (a.getData().isBefore(inicioMes) || a.getData().isAfter(fimMes)) continue;
            String nome = a.getProfissional().getNome();
            atendimentosPorProfissional.merge(nome, 1L, Long::sum);
            clientesPorProfissional.computeIfAbsent(nome, k -> new HashSet<>()).add(a.getCliente().getId());
        }

        Map<String, BigDecimal> faturamentoPorProfissional = new HashMap<>();
        for (Receita r : receitasMesAtual) {
            if (r.getProfissional() == null) continue;
            faturamentoPorProfissional.merge(r.getProfissional().getNome(), r.getValor(), BigDecimal::add);
        }

        Map<String, BigDecimal> comissaoPorProfissional = new HashMap<>();
        for (Comissao c : comissaoRepository.findAllByBarbeariaId(barbeariaId)) {
            if (c.getData().isBefore(inicioMes) || c.getData().isAfter(fimMes)) continue;
            comissaoPorProfissional.merge(c.getProfissional().getNome(), c.getValor(), BigDecimal::add);
        }

        List<DashboardResponse.ProfissionalDesempenho> profissionais = new ArrayList<>();
        for (String nome : atendimentosPorProfissional.keySet()) {
            long qtd = atendimentosPorProfissional.get(nome);
            BigDecimal faturamento = faturamentoPorProfissional.getOrDefault(nome, BigDecimal.ZERO);
            BigDecimal comissao = comissaoPorProfissional.getOrDefault(nome, BigDecimal.ZERO);
            BigDecimal ticketMedio = qtd == 0 ? BigDecimal.ZERO : faturamento.divide(BigDecimal.valueOf(qtd), 2, RoundingMode.HALF_UP);
            long clientesAtendidos = clientesPorProfissional.getOrDefault(nome, Set.of()).size();
            profissionais.add(new DashboardResponse.ProfissionalDesempenho(nome, qtd, faturamento, comissao, ticketMedio, clientesAtendidos));
        }
        profissionais.sort((a, b) -> b.getFaturamento().compareTo(a.getFaturamento()));
        return profissionais.size() > TOP_N ? profissionais.subList(0, TOP_N) : profissionais;
    }

    private List<DashboardResponse.ServicoDesempenho> montarRankingServicos(
            List<Agendamento> todosAgendamentos, List<Receita> receitasMesAtual, LocalDate inicioMes, LocalDate fimMes) {

        Map<String, Long> qtdPorServico = new LinkedHashMap<>();
        for (Agendamento a : todosAgendamentos) {
            if (a.getStatus() != StatusAgendamento.CONCLUIDO) continue;
            if (a.getData().isBefore(inicioMes) || a.getData().isAfter(fimMes)) continue;
            qtdPorServico.merge(a.getServico().getNome(), 1L, Long::sum);
        }

        Map<String, BigDecimal> faturamentoPorServico = new HashMap<>();
        for (Receita r : receitasMesAtual) {
            if (r.getServico() == null) continue;
            faturamentoPorServico.merge(r.getServico().getNome(), r.getValor(), BigDecimal::add);
        }

        List<DashboardResponse.ServicoDesempenho> servicos = new ArrayList<>();
        for (String nome : qtdPorServico.keySet()) {
            long qtd = qtdPorServico.get(nome);
            BigDecimal faturamento = faturamentoPorServico.getOrDefault(nome, BigDecimal.ZERO);
            BigDecimal ticketMedio = qtd == 0 ? BigDecimal.ZERO : faturamento.divide(BigDecimal.valueOf(qtd), 2, RoundingMode.HALF_UP);
            servicos.add(new DashboardResponse.ServicoDesempenho(nome, qtd, faturamento, ticketMedio));
        }
        servicos.sort((a, b) -> b.getFaturamento().compareTo(a.getFaturamento()));
        return servicos.size() > TOP_N ? servicos.subList(0, TOP_N) : servicos;
    }

    private DashboardResponse.ClientesResumo montarResumoClientes(
            List<Cliente> clientes, List<Agendamento> todosAgendamentos, LocalDate hoje,
            long novosClientesMes, long clientesRecorrentesMes) {

        LocalDate limite30Dias = hoje.minusDays(30);
        Map<Long, LocalDate> ultimoAgendamentoPorCliente = new HashMap<>();
        for (Agendamento a : todosAgendamentos) {
            if (a.getStatus() == StatusAgendamento.CANCELADO) continue;
            Long clienteId = a.getCliente().getId();
            LocalDate atual = ultimoAgendamentoPorCliente.get(clienteId);
            if (atual == null || a.getData().isAfter(atual)) {
                ultimoAgendamentoPorCliente.put(clienteId, a.getData());
            }
        }

        List<DashboardResponse.ClienteInativo> inativos = new ArrayList<>();
        for (Cliente c : clientes) {
            LocalDate ultimo = ultimoAgendamentoPorCliente.get(c.getId());
            if (ultimo == null || ultimo.isBefore(limite30Dias)) {
                inativos.add(new DashboardResponse.ClienteInativo(c.getNome(), c.getTelefone(),
                        ultimo == null ? null : ultimo.toString()));
            }
        }
        inativos.sort(Comparator.comparing(
                DashboardResponse.ClienteInativo::getUltimoAgendamento,
                Comparator.nullsFirst(Comparator.naturalOrder())));

        long totalInativos = inativos.size();
        List<DashboardResponse.ClienteInativo> topInativos = inativos.size() > TOP_N ? inativos.subList(0, TOP_N) : inativos;

        return new DashboardResponse.ClientesResumo(novosClientesMes, clientesRecorrentesMes, totalInativos, topInativos);
    }

    private long contarNoPeriodo(List<Agendamento> agendamentos, LocalDate inicio, LocalDate fim) {
        return agendamentos.stream()
                .filter(a -> a.getStatus() != StatusAgendamento.CANCELADO)
                .filter(a -> !a.getData().isBefore(inicio) && !a.getData().isAfter(fim))
                .count();
    }

    private List<Receita> receitasRecebidas(Long barbeariaId, LocalDate inicio, LocalDate fim) {
        return receitaRepository.findAllByBarbeariaIdAndDataBetween(barbeariaId, inicio, fim).stream()
                .filter(r -> r.getStatus() == StatusReceita.RECEBIDO)
                .toList();
    }

    private BigDecimal soma(List<Receita> receitas) {
        return receitas.stream().map(Receita::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Null quando não há base de comparação (período anterior com faturamento zero). */
    private Double variacaoPercentual(BigDecimal atual, BigDecimal anterior) {
        if (anterior.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return atual.subtract(anterior)
                .divide(anterior, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
