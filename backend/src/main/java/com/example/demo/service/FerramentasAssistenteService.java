package com.example.demo.service;

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
import com.example.demo.repository.ProfissionalRepository;
import com.example.demo.repository.ReceitaRepository;
import com.example.demo.repository.ServicoRepository;
import com.example.demo.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * "Ferramentas" que o Assistente de IA pode chamar — nunca acesso livre ao banco.
 * Toda consulta aqui é implicitamente isolada pela barbearia do usuário autenticado
 * (via SecurityUtils.barbeariaAtualId()); a IA NUNCA recebe nem controla esse valor.
 * Todo número devolvido vem direto do banco — nada é estimado ou inventado aqui.
 */
@Service
@RequiredArgsConstructor
public class FerramentasAssistenteService {

    private final ReceitaRepository receitaRepository;
    private final DespesaRepository despesaRepository;
    private final ComissaoRepository comissaoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final ClienteRepository clienteRepository;
    private final ProfissionalRepository profissionalRepository;
    private final ServicoRepository servicoRepository;

    private record Intervalo(LocalDate inicio, LocalDate fim, String rotulo) {}

    private Intervalo resolverPeriodo(String periodo) {
        LocalDate hoje = LocalDate.now();
        String chave = periodo == null ? "" : periodo.trim().toLowerCase(Locale.ROOT);
        return switch (chave) {
            case "hoje" -> new Intervalo(hoje, hoje, "hoje");
            case "ontem" -> {
                LocalDate ontem = hoje.minusDays(1);
                yield new Intervalo(ontem, ontem, "ontem");
            }
            case "semana_atual" -> new Intervalo(hoje.with(DayOfWeek.MONDAY), hoje, "semana atual");
            case "semana_passada" -> {
                LocalDate inicioAtual = hoje.with(DayOfWeek.MONDAY);
                yield new Intervalo(inicioAtual.minusWeeks(1), inicioAtual.minusDays(1), "semana passada");
            }
            case "mes_passado" -> {
                LocalDate mesPassado = hoje.minusMonths(1);
                yield new Intervalo(mesPassado.withDayOfMonth(1),
                        mesPassado.withDayOfMonth(mesPassado.lengthOfMonth()), "mês passado");
            }
            default -> new Intervalo(hoje.withDayOfMonth(1), hoje, "mês atual");
        };
    }

    public Map<String, Object> obterFaturamento(String periodo) {
        Long barbeariaId = SecurityUtils.barbeariaAtualId();
        Intervalo intervalo = resolverPeriodo(periodo);

        List<Receita> receitas = receitaRepository
                .findAllByBarbeariaIdAndDataBetween(barbeariaId, intervalo.inicio(), intervalo.fim()).stream()
                .filter(r -> r.getStatus() == StatusReceita.RECEBIDO)
                .toList();

        BigDecimal total = receitas.stream().map(Receita::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ticketMedio = receitas.isEmpty()
                ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(receitas.size()), 2, RoundingMode.HALF_UP);

        Map<String, BigDecimal> porFormaPagamento = new LinkedHashMap<>();
        for (FormaPagamento forma : FormaPagamento.values()) {
            BigDecimal soma = receitas.stream()
                    .filter(r -> r.getFormaPagamento() == forma)
                    .map(Receita::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (soma.compareTo(BigDecimal.ZERO) > 0) {
                porFormaPagamento.put(forma.name(), soma);
            }
        }

        Map<String, BigDecimal> faturamentoPorDia = new TreeMap<>();
        for (Receita r : receitas) {
            faturamentoPorDia.merge(r.getData().toString(), r.getValor(), BigDecimal::add);
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("periodo", intervalo.rotulo());
        resultado.put("dataInicio", intervalo.inicio().toString());
        resultado.put("dataFim", intervalo.fim().toString());
        resultado.put("totalFaturado", total);
        resultado.put("quantidadeAtendimentosPagos", receitas.size());
        resultado.put("ticketMedio", ticketMedio);
        resultado.put("faturamentoPorFormaPagamento", porFormaPagamento);
        resultado.put("faturamentoPorDia", faturamentoPorDia);
        return resultado;
    }

    public Map<String, Object> obterAtendimentos(String periodo) {
        Long barbeariaId = SecurityUtils.barbeariaAtualId();
        Intervalo intervalo = resolverPeriodo(periodo);

        List<Agendamento> agendamentos = agendamentoRepository.findAllByBarbeariaId(barbeariaId).stream()
                .filter(a -> !a.getData().isBefore(intervalo.inicio()) && !a.getData().isAfter(intervalo.fim()))
                .toList();

        long total = agendamentos.size();
        long concluidos = agendamentos.stream().filter(a -> a.getStatus() == StatusAgendamento.CONCLUIDO).count();
        long cancelados = agendamentos.stream().filter(a -> a.getStatus() == StatusAgendamento.CANCELADO).count();

        Map<String, Long> porProfissional = new LinkedHashMap<>();
        Map<String, Long> porServico = new LinkedHashMap<>();
        for (Agendamento a : agendamentos) {
            if (a.getStatus() != StatusAgendamento.CONCLUIDO) continue;
            porProfissional.merge(a.getProfissional().getNome(), 1L, Long::sum);
            porServico.merge(a.getServico().getNome(), 1L, Long::sum);
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("periodo", intervalo.rotulo());
        resultado.put("totalAgendamentos", total);
        resultado.put("concluidos", concluidos);
        resultado.put("cancelados", cancelados);
        resultado.put("atendimentosConcluidosPorProfissional", porProfissional);
        resultado.put("atendimentosConcluidosPorServico", porServico);
        return resultado;
    }

    public Map<String, Object> obterComissoes(String periodo) {
        Long barbeariaId = SecurityUtils.barbeariaAtualId();
        Intervalo intervalo = resolverPeriodo(periodo);

        List<Comissao> comissoes = comissaoRepository.findAllByBarbeariaId(barbeariaId).stream()
                .filter(c -> !c.getData().isBefore(intervalo.inicio()) && !c.getData().isAfter(intervalo.fim()))
                .toList();

        Map<String, BigDecimal> total = new LinkedHashMap<>();
        Map<String, BigDecimal> paga = new LinkedHashMap<>();
        Map<String, BigDecimal> pendente = new LinkedHashMap<>();
        for (Comissao c : comissoes) {
            String nome = c.getProfissional().getNome();
            total.merge(nome, c.getValor(), BigDecimal::add);
            if (c.isPaga()) {
                paga.merge(nome, c.getValor(), BigDecimal::add);
            } else {
                pendente.merge(nome, c.getValor(), BigDecimal::add);
            }
        }

        List<Map<String, Object>> porProfissional = new ArrayList<>();
        for (String nome : total.keySet()) {
            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("profissional", nome);
            linha.put("comissaoTotal", total.get(nome));
            linha.put("comissaoPaga", paga.getOrDefault(nome, BigDecimal.ZERO));
            linha.put("comissaoPendente", pendente.getOrDefault(nome, BigDecimal.ZERO));
            porProfissional.add(linha);
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("periodo", intervalo.rotulo());
        resultado.put("comissaoPorProfissional", porProfissional);
        return resultado;
    }

    public Map<String, Object> obterDespesas(String periodo) {
        Long barbeariaId = SecurityUtils.barbeariaAtualId();
        Intervalo intervalo = resolverPeriodo(periodo);

        List<Despesa> despesas = despesaRepository
                .findAllByBarbeariaIdAndDataBetween(barbeariaId, intervalo.inicio(), intervalo.fim());

        BigDecimal total = despesas.stream().map(Despesa::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> porCategoria = new LinkedHashMap<>();
        for (Despesa d : despesas) {
            String categoria = (d.getCategoria() == null || d.getCategoria().isBlank()) ? "Sem categoria" : d.getCategoria();
            porCategoria.merge(categoria, d.getValor(), BigDecimal::add);
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("periodo", intervalo.rotulo());
        resultado.put("totalDespesas", total);
        resultado.put("despesasPorCategoria", porCategoria);
        return resultado;
    }

    public Map<String, Object> obterClientes() {
        Long barbeariaId = SecurityUtils.barbeariaAtualId();
        List<Cliente> clientes = clienteRepository.findAllByBarbeariaId(barbeariaId);

        LocalDate limite30Dias = LocalDate.now().minusDays(30);
        LocalDateTime ha30Dias = limite30Dias.atStartOfDay();

        long novosUltimos30Dias = clientes.stream()
                .filter(c -> c.getDataCadastro() != null && c.getDataCadastro().isAfter(ha30Dias))
                .count();

        Map<Long, LocalDate> ultimoAgendamentoPorCliente = new HashMap<>();
        for (Agendamento a : agendamentoRepository.findAllByBarbeariaId(barbeariaId)) {
            if (a.getStatus() == StatusAgendamento.CANCELADO) continue;
            Long clienteId = a.getCliente().getId();
            LocalDate atual = ultimoAgendamentoPorCliente.get(clienteId);
            if (atual == null || a.getData().isAfter(atual)) {
                ultimoAgendamentoPorCliente.put(clienteId, a.getData());
            }
        }

        List<Map<String, Object>> inativos = new ArrayList<>();
        for (Cliente c : clientes) {
            LocalDate ultimo = ultimoAgendamentoPorCliente.get(c.getId());
            if (ultimo == null || ultimo.isBefore(limite30Dias)) {
                Map<String, Object> linha = new LinkedHashMap<>();
                linha.put("nome", c.getNome());
                linha.put("telefone", c.getTelefone());
                linha.put("ultimoAgendamento", ultimo == null ? "nunca agendou" : ultimo.toString());
                inativos.add(linha);
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("totalClientes", clientes.size());
        resultado.put("novosUltimos30Dias", novosUltimos30Dias);
        resultado.put("clientesSemAgendamentoHa30DiasOuMais", inativos);
        return resultado;
    }

    public List<Map<String, Object>> obterProfissionais() {
        Long barbeariaId = SecurityUtils.barbeariaAtualId();
        return profissionalRepository.findAllByBarbeariaId(barbeariaId).stream()
                .map(p -> {
                    Map<String, Object> linha = new LinkedHashMap<>();
                    linha.put("nome", p.getNome());
                    linha.put("funcao", p.getFuncao());
                    linha.put("percentualComissao", p.getPercentualComissao());
                    linha.put("ativo", p.isAtivo());
                    return linha;
                })
                .toList();
    }

    public List<Map<String, Object>> obterServicos() {
        Long barbeariaId = SecurityUtils.barbeariaAtualId();
        return servicoRepository.findAllByBarbeariaId(barbeariaId).stream()
                .map(s -> {
                    Map<String, Object> linha = new LinkedHashMap<>();
                    linha.put("nome", s.getNome());
                    linha.put("preco", s.getPreco());
                    linha.put("duracaoMinutos", s.getDuracaoMinutos());
                    linha.put("ativo", s.isAtivo());
                    return linha;
                })
                .toList();
    }
}
