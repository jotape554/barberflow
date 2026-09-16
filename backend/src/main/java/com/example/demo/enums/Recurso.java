package com.example.demo.enums;

import lombok.Getter;

/**
 * Catálogo central de "recursos" do BarberFlow e o plano mínimo que dá acesso a cada um.
 * Único lugar que precisa mudar para reclassificar um recurso entre planos, ou para
 * cadastrar um recurso novo (basta adicionar aqui e apontar o plano mínimo desejado).
 *
 * A checagem de acesso vive em AssinaturaService.recursoLiberado(...); a aplicação prática
 * na API acontece em RecursoGateFilter.
 */
@Getter
public enum Recurso {
    AGENDA(PlanoSaas.BASICO),
    CLIENTES(PlanoSaas.BASICO),
    SERVICOS(PlanoSaas.BASICO),
    PROFISSIONAIS(PlanoSaas.BASICO),

    FINANCEIRO(PlanoSaas.PROFISSIONAL),
    DASHBOARD(PlanoSaas.PROFISSIONAL),
    COMISSOES(PlanoSaas.PROFISSIONAL),
    AGENDAMENTO_PUBLICO(PlanoSaas.PROFISSIONAL),
    RELATORIOS(PlanoSaas.PROFISSIONAL),

    ASSISTENTE_IA(PlanoSaas.PREMIUM);

    private final PlanoSaas planoMinimo;

    Recurso(PlanoSaas planoMinimo) {
        this.planoMinimo = planoMinimo;
    }
}
