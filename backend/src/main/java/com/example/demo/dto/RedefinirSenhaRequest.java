package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RedefinirSenhaRequest {

    @NotBlank
    private String token;

    @NotBlank
    private String novaSenha;
}
