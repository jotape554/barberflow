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
    BASICO(new BigDecimal("69.90"), "1 profissional, agenda, clientes e serviços", 1),
    PROFISSIONAL(new BigDecimal("129.90"), "Até 5 profissionais, financeiro completo, comissões e dashboard", 5),
    PREMIUM(new BigDecimal("199.90"), "Profissionais ilimitados, Assistente de IA e suporte prioritário", null);

    private final BigDecimal precoMensal;
    private final String descricao;
    /** Número máximo de profissionais que a barbearia pode cadastrar neste plano; null = sem limite. */
    private final Integer limiteProfissionais;

    PlanoSaas(BigDecimal precoMensal, String descricao, Integer limiteProfissionais) {
        this.precoMensal = precoMensal;
        this.descricao = descricao;
        this.limiteProfissionais = limiteProfissionais;
    }

    /**
     * Os planos são cumulativos por ordem de declaração (BASICO < PROFISSIONAL < PREMIUM):
     * quem está num plano mais alto automaticamente atende os requisitos dos planos abaixo.
     */
    public boolean atendeNivelMinimo(PlanoSaas minimo) {
        return this.ordinal() >= minimo.ordinal();
    }
}
