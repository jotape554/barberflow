package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Resposta pública de Profissional. telefone/email/percentualComissao vêm nulos quando quem
 * pergunta é outro profissional (não o dono da conta) — ver ProfissionalService.paraResponse.
 */
@Data
@Builder
@AllArgsConstructor
public class ProfissionalResponse {
    private Long id;
    private String nome;
    private String fotoUrl;
    private String telefone;
    private String email;
    private String funcao;
    private String diasTrabalho;
    private String horarioInicio;
    private String horarioFim;
    private BigDecimal percentualComissao;
    private boolean ativo;
}
