package com.example.demo.dto;

import com.example.demo.model.Despesa;
import com.example.demo.model.Receita;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Relatório financeiro de um período, já filtrado por profissional/serviço/forma de pagamento
 * quando informados. resultadoLiquido é uma estimativa (faturamento - despesas - comissões),
 * não um fechamento contábil.
 */
@Data
@AllArgsConstructor
public class RelatorioResponse {
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private BigDecimal faturamentoBruto;
    private BigDecimal totalDespesas;
    private BigDecimal totalComissoes;
    private BigDecimal resultadoLiquido;
    private List<Receita> receitas;
    private List<Despesa> despesas;
}
