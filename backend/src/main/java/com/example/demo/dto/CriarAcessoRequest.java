package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CriarAcessoRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String senha;
}
