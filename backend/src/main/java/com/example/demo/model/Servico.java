package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "servicos", indexes = @Index(name = "idx_servicos_barbearia", columnList = "barbearia_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbearia_id", nullable = false)
    private Barbearia barbearia;

    @NotBlank(message = "Informe o nome do serviço.")
    @Column(nullable = false)
    private String nome;

    private String descricao;

    @NotNull(message = "Informe o preço do serviço.")
    @Positive(message = "O preço precisa ser maior que zero.")
    @Column(nullable = false)
    private BigDecimal preco;

    @NotNull(message = "Informe a duração do serviço.")
    @Min(value = 1, message = "A duração precisa ser de pelo menos 1 minuto.")
    @Column(nullable = false)
    private Integer duracaoMinutos;

    @Builder.Default
    private boolean ativo = true;
}
