package com.example.demo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Limita quantos agendamentos por minuto o MESMO endereço IP consegue criar pela página
 * pública (sem login) — sem isso, um script poderia lotar a agenda de uma barbearia com
 * clientes/horários falsos. Implementação em memória (sem dependência nova): adequada
 * enquanto o backend roda numa única instância; se um dia escalar para várias instâncias,
 * precisaria virar um limitador compartilhado (ex.: Redis).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int LIMITE_REQUISICOES = 10;
    private static final long JANELA_MS = Duration.ofMinutes(1).toMillis();
    private static final int TAMANHO_MAXIMO_MAPA = 10_000;

    private record Contador(AtomicInteger quantidade, AtomicLong inicioJanelaEpochMs) {}

    private final Map<String, Contador> contadoresPorIp = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!ehCriacaoDeAgendamentoPublico(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (excedeuLimite(obterIp(request))) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("status", 429);
            corpo.put("mensagem", "Muitas tentativas de agendamento em pouco tempo. Aguarde um instante e tente de novo.");
            objectMapper.writeValue(response.getWriter(), corpo);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean ehCriacaoDeAgendamentoPublico(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().startsWith("/public/barbearias/")
                && request.getRequestURI().endsWith("/agendamentos");
    }

    private boolean excedeuLimite(String ip) {
        long agora = System.currentTimeMillis();
        limparEntradasAntigasSeNecessario(agora);

        Contador contador = contadoresPorIp.computeIfAbsent(ip, k -> new Contador(new AtomicInteger(0), new AtomicLong(agora)));

        synchronized (contador) {
            if (agora - contador.inicioJanelaEpochMs().get() > JANELA_MS) {
                contador.inicioJanelaEpochMs().set(agora);
                contador.quantidade().set(0);
            }
            return contador.quantidade().incrementAndGet() > LIMITE_REQUISICOES;
        }
    }

    private void limparEntradasAntigasSeNecessario(long agora) {
        if (contadoresPorIp.size() < TAMANHO_MAXIMO_MAPA) return;
        contadoresPorIp.entrySet().removeIf(entry -> agora - entry.getValue().inicioJanelaEpochMs().get() > JANELA_MS * 2);
    }

    private String obterIp(HttpServletRequest request) {
        String encaminhado = request.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            return encaminhado.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
