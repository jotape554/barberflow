package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "despesas", indexes = @Index(name = "idx_despesas_barbearia", columnList = "barbearia_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Despesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbearia_id", nullable = false)
    private Barbearia barbearia;

    @NotBlank(message = "Informe a descrição da despesa.")
    @Column(nullable = false)
    private String descricao;

    private String categoria;

    @NotNull(message = "Informe o valor da despesa.")
    @Positive(message = "O valor precisa ser maior que zero.")
    @Column(nullable = false)
    private BigDecimal valor;

    @NotNull(message = "Informe a data da despesa.")
    @Column(nullable = false)
    private LocalDate data;

    private String observacao;
}
