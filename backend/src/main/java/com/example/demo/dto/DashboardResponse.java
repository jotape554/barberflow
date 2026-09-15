package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class DashboardResponse {
    private BigDecimal faturamentoHoje;
    private BigDecimal faturamentoMes;
    private long agendamentosHoje;
    private long agendamentosConcluidosHoje;
    private long clientesCadastrados;
    private BigDecimal ticketMedioMes;
}
