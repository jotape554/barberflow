package com.example.demo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private Map<String, Object> corpo(HttpStatus status, String mensagem) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("timestamp", LocalDateTime.now());
        corpo.put("status", status.value());
        corpo.put("erro", status.getReasonPhrase());
        corpo.put("mensagem", mensagem);
        return corpo;
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(corpo(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(ConflitoHorarioException.class)
    public ResponseEntity<Map<String, Object>> handleConflito(ConflitoHorarioException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(corpo(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<Map<String, Object>> handleRegraNegocio(RegraDeNegocioException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(corpo(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleCredenciais(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(corpo(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenerico(Exception ex) {
        log.error("Erro interno não tratado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(corpo(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno: " + ex.getMessage()));
    }
}
