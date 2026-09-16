package com.example.demo.model;

import com.example.demo.enums.PlanoSaas;
import com.example.demo.enums.StatusAssinaturaSaas;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "barbearias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Barbearia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    /** usado na URL pública: /barbearia/{slug} */
    @Column(nullable = false, unique = true)
    private String slug;

    private String descricao;
    private String endereco;
    private String whatsapp;
    private String instagram;
    private String horarioFuncionamento;
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PlanoSaas planoSaas = PlanoSaas.BASICO;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatusAssinaturaSaas statusAssinaturaSaas = StatusAssinaturaSaas.TRIAL;

    /** Fim do período de teste gratuito. Após essa data, sem status ATIVA, o acesso ao painel é bloqueado. */
    private LocalDate dataFimTrial;

    /**
     * Preenchidos após o primeiro checkout na Stripe — usados para reconciliar os eventos do
     * webhook. Nunca devem sair pro cliente: Barbearia aparece aninhada em respostas de
     * Cliente/Serviço/Profissional/Agendamento/etc., então isso vazaria pra qualquer usuário
     * autenticado sem necessidade nenhuma.
     */
    @JsonIgnore
    private String stripeCustomerId;
    @JsonIgnore
    private String stripeSubscriptionId;

    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();
}
