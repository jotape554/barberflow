package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "profissionais", indexes = @Index(name = "idx_profissionais_barbearia", columnList = "barbearia_id"))
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

    @NotBlank(message = "Informe o nome do profissional.")
    @Column(nullable = false)
    private String nome;

    private String fotoUrl;
    private String telefone;

    @Email(message = "Informe um e-mail válido.")
    private String email;
    private String funcao;

    /** ex: "SEG,TER,QUA,QUI,SEX" */
    private String diasTrabalho;
    private String horarioInicio;
    private String horarioFim;

    @NotNull(message = "Informe o percentual de comissão.")
    @DecimalMin(value = "0", message = "O percentual de comissão não pode ser negativo.")
    @DecimalMax(value = "100", message = "O percentual de comissão não pode passar de 100%.")
    @Builder.Default
    private BigDecimal percentualComissao = BigDecimal.ZERO;

    @Builder.Default
    private boolean ativo = true;
}
