package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "clientes", indexes = {
        @Index(name = "idx_clientes_barbearia", columnList = "barbearia_id"),
        @Index(name = "idx_clientes_barbearia_telefone", columnList = "barbearia_id, telefone")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbearia_id", nullable = false)
    private Barbearia barbearia;

    @Column(nullable = false)
    private String nome;

    private String telefone;
    private String whatsapp;
    private String email;

    @Column(length = 1000)
    private String observacoes;

    @Builder.Default
    private LocalDateTime dataCadastro = LocalDateTime.now();
}
