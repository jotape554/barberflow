package com.example.demo.service;

import com.example.demo.dto.RelatorioResponse;
import com.example.demo.enums.FormaPagamento;
import com.example.demo.enums.StatusReceita;
import com.example.demo.exception.RegraDeNegocioException;
import com.example.demo.model.Comissao;
import com.example.demo.model.Despesa;
import com.example.demo.model.Receita;
import com.example.demo.repository.ComissaoRepository;
import com.example.demo.repository.DespesaRepository;
import com.example.demo.repository.ReceitaRepository;
import com.example.demo.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Relatório com filtro por período, profissional, serviço e forma de pagamento. Todo número
 * vem de uma soma direta dos lançamentos filtrados — nada estimado além do próprio
 * resultadoLiquido, que é explicitamente uma aproximação (não substitui contabilidade).
 */
@Service
@RequiredArgsConstructor
public class RelatorioService {

    private final ReceitaRepository receitaRepository;
    private final DespesaRepository despesaRepository;
    private final ComissaoRepository comissaoRepository;

    public RelatorioResponse gerar(LocalDate dataInicio, LocalDate dataFim, Long profissionalId,
                                    Long servicoId, FormaPagamento formaPagamento) {
        if (dataFim.isBefore(dataInicio)) {
            throw new RegraDeNegocioException("A data final não pode ser anterior à data inicial.");
        }

        Long barbeariaId = SecurityUtils.barbeariaAtualId();

        List<Receita> receitas = receitaRepository.findAllByBarbeariaIdAndDataBetween(barbeariaId, dataInicio, dataFim).stream()
                .filter(r -> r.getStatus() == StatusReceita.RECEBIDO)
                .filter(r -> profissionalId == null || (r.getProfissional() != null && profissionalId.equals(r.getProfissional().getId())))
                .filter(r -> servicoId == null || (r.getServico() != null && servicoId.equals(r.getServico().getId())))
                .filter(r -> formaPagamento == null || r.getFormaPagamento() == formaPagamento)
                .toList();

        List<Despesa> despesas = despesaRepository.findAllByBarbeariaIdAndDataBetween(barbeariaId, dataInicio, dataFim);

        BigDecimal faturamentoBruto = receitas.stream().map(Receita::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDespesas = despesas.stream().map(Despesa::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalComissoes = comissaoRepository.findAllByBarbeariaId(barbeariaId).stream()
                .filter(c -> !c.getData().isBefore(dataInicio) && !c.getData().isAfter(dataFim))
                .filter(c -> profissionalId == null || profissionalId.equals(c.getProfissional().getId()))
                .map(Comissao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal resultadoLiquido = faturamentoBruto.subtract(totalDespesas).subtract(totalComissoes);

        return new RelatorioResponse(dataInicio, dataFim, faturamentoBruto, totalDespesas, totalComissoes, resultadoLiquido, receitas, despesas);
    }
}
