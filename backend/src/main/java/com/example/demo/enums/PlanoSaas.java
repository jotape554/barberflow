package com.example.demo.enums;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Plano de assinatura da BARBEARIA dentro do BarberPro (o SaaS em si).
 * Não confundir com o plano que a barbearia oferece para os próprios clientes
 * (ex: corte ilimitado) — isso é outra entidade, referente ao produto da barbearia.
 */
@Getter
public enum PlanoSaas {
    BASICO(new BigDecimal("59.90"), "1 profissional, agenda e financeiro básico"),
    PROFISSIONAL(new BigDecimal("119.90"), "Até 5 profissionais, agendamento público e comissões"),
    PREMIUM(new BigDecimal("199.90"), "Profissionais ilimitados e suporte prioritário");

    private final BigDecimal precoMensal;
    private final String descricao;

    PlanoSaas(BigDecimal precoMensal, String descricao) {
        this.precoMensal = precoMensal;
        this.descricao = descricao;
    }
}
