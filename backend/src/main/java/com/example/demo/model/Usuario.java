package com.example.demo.model;

import com.example.demo.enums.Papel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "cpf")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String email;

    /** Só preenchido para o administrador que criou a conta (evita reuso de trial com CPFs diferentes). */
    private String cpf;

    @Column(nullable = false)
    @JsonIgnore
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Papel papel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbearia_id", nullable = false)
    private Barbearia barbearia;

    /** Só preenchido quando papel = PROFISSIONAL — é o que restringe o que esse login enxerga. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profissional_id")
    private Profissional profissional;

    @Builder.Default
    private boolean ativo = true;

    /** Preenchidos só durante um pedido de "esqueci minha senha" em andamento. */
    @JsonIgnore
    private String resetSenhaToken;
    private Instant resetSenhaExpiraEm;
}
