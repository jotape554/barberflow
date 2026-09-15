package com.example.demo.security;

import org.springframework.security.core.context.SecurityContextHolder;

/** Ponto único para descobrir o usuário/barbearia autenticados em qualquer service/controller. */
public class SecurityUtils {

    private SecurityUtils() {}

    public static UsuarioPrincipal usuarioAtual() {
        return (UsuarioPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public static Long barbeariaAtualId() {
        return usuarioAtual().getBarbeariaId();
    }
}
