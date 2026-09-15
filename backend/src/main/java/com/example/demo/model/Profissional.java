package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "profissionais")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profissional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbearia_id", nullable = false)
    private Barbearia barbearia;

    @Column(nullable = false)
    private String nome;

    private String fotoUrl;
    private String telefone;
    private String email;
    private String funcao;

    /** ex: "SEG,TER,QUA,QUI,SEX" */
    private String diasTrabalho;
    private String horarioInicio;
    private String horarioFim;

    @Builder.Default
    private BigDecimal percentualComissao = BigDecimal.ZERO;

    @Builder.Default
    private boolean ativo = true;
}
