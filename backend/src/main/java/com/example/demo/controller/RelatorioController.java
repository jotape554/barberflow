package com.example.demo.controller;

import com.example.demo.dto.RelatorioResponse;
import com.example.demo.enums.FormaPagamento;
import com.example.demo.service.RelatorioService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Recurso do plano Profissional ou superior — ver Recurso.RELATORIOS e RecursoGateFilter. */
@RestController
@RequestMapping("/api/relatorios")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR','GERENTE')")
public class RelatorioController {

    private final RelatorioService relatorioService;

    @GetMapping
    public RelatorioResponse gerar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Long profissionalId,
            @RequestParam(required = false) Long servicoId,
            @RequestParam(required = false) FormaPagamento formaPagamento) {
        return relatorioService.gerar(dataInicio, dataFim, profissionalId, servicoId, formaPagamento);
    }
}
