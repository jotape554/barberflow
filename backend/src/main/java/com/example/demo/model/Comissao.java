package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "comissoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comissao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbearia_id", nullable = false)
    private Barbearia barbearia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profissional_id", nullable = false)
    private Profissional profissional;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id", unique = true)
    private Agendamento agendamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receita_id")
    private Receita receita;

    @Column(nullable = false)
    private BigDecimal percentual;

    @Column(nullable = false)
    private BigDecimal valor;

    @Builder.Default
    private boolean paga = false;

    @Column(nullable = false)
    private LocalDate data;
}
