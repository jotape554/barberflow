package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Cadastro de uma NOVA barbearia (conta) + seu usuário administrador. */
@Data
public class RegistroRequest {

    @NotBlank
    private String nomeBarbearia;

    @NotBlank
    private String nomeAdmin;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String senha;
}
