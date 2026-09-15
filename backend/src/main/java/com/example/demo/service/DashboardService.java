package com.example.demo.service;

import com.example.demo.dto.DashboardResponse;
import com.example.demo.enums.StatusAgendamento;
import com.example.demo.enums.StatusReceita;
import com.example.demo.model.Agendamento;
import com.example.demo.model.Receita;
import com.example.demo.repository.AgendamentoRepository;
import com.example.demo.repository.ClienteRepository;
import com.example.demo.repository.ReceitaRepository;
import com.example.demo.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Todos os números aqui vêm direto do banco. Sem dado fictício:
 * se não houver receita/agendamento no período, o valor retornado é zero
 * e cabe ao frontend mostrar o estado vazio ("ainda não há dados").
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ReceitaRepository receitaRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final ClienteRepository clienteRepository;

    public DashboardResponse gerar() {
        Long barbeariaId = SecurityUtils.barbeariaAtualId();
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth());

        List<Receita> receitasHoje = receitaRepository.findAllByBarbeariaIdAndDataBetween(barbeariaId, hoje, hoje);
        List<Receita> receitasMes = receitaRepository.findAllByBarbeariaIdAndDataBetween(barbeariaId, inicioMes, fimMes);

        BigDecimal faturamentoHoje = somaRecebido(receitasHoje);
        BigDecimal faturamentoMes = somaRecebido(receitasMes);

        List<Agendamento> agendamentosHoje = agendamentoRepository.findAllByBarbeariaIdAndData(barbeariaId, hoje);
        long totalAgendamentosHoje = agendamentosHoje.stream()
                .filter(a -> a.getStatus() != StatusAgendamento.CANCELADO)
                .count();
        long concluidosHoje = agendamentosHoje.stream()
                .filter(a -> a.getStatus() == StatusAgendamento.CONCLUIDO)
                .count();

        long clientesCadastrados = clienteRepository.findAllByBarbeariaId(barbeariaId).size();

        long qtdReceitasRecebidasMes = receitasMes.stream()
                .filter(r -> r.getStatus() == StatusReceita.RECEBIDO)
                .count();

        BigDecimal ticketMedio = qtdReceitasRecebidasMes == 0
                ? BigDecimal.ZERO
                : faturamentoMes.divide(BigDecimal.valueOf(qtdReceitasRecebidasMes), 2, RoundingMode.HALF_UP);

        return new DashboardResponse(faturamentoHoje, faturamentoMes, totalAgendamentosHoje,
                concluidosHoje, clientesCadastrados, ticketMedio);
    }

    private BigDecimal somaRecebido(List<Receita> receitas) {
        return receitas.stream()
                .filter(r -> r.getStatus() == StatusReceita.RECEBIDO)
                .map(Receita::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
