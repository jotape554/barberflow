package com.example.demo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.model.Barbearia;
import com.example.demo.repository.BarbeariaRepository;
import com.example.demo.service.AssinaturaService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bloqueia o painel administrativo (não a agenda pública de clientes) quando o período de
 * teste da barbearia expira e nenhum plano foi ativado. Roda depois do JwtAuthFilter, então
 * já existe (ou não) uma autenticação no SecurityContext quando este filtro é executado.
 */
@Component
@RequiredArgsConstructor
public class AssinaturaGateFilter extends OncePerRequestFilter {

    private final BarbeariaRepository barbeariaRepository;
    private final AssinaturaService assinaturaService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        boolean isolado = !path.startsWith("/api/") || path.startsWith("/api/assinatura");
        if (isolado) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth != null && auth.getPrincipal() instanceof UsuarioPrincipal principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        Barbearia barbearia = barbeariaRepository.findById(principal.getBarbeariaId()).orElse(null);
        if (barbearia == null || assinaturaService.acessoLiberado(barbearia)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(402); // Payment Required
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("timestamp", LocalDateTime.now());
        corpo.put("status", 402);
        corpo.put("erro", "Payment Required");
        corpo.put("mensagem", "Seu período de teste terminou. Escolha um plano para continuar usando o BarberFlow.");
        objectMapper.writeValue(response.getWriter(), corpo);
    }
}
