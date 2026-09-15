package com.example.demo.controller;

import com.example.demo.enums.FormaPagamento;
import com.example.demo.enums.StatusAgendamento;
import com.example.demo.model.Agendamento;
import com.example.demo.service.AgendamentoService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/agendamentos")
@RequiredArgsConstructor
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    @GetMapping
    public List<Agendamento> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return data != null ? agendamentoService.listarPorData(data) : agendamentoService.listar();
    }

    @GetMapping("/{id}")
    public Agendamento buscar(@PathVariable Long id) {
        return agendamentoService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Agendamento> criar(@RequestBody NovoAgendamentoRequest req) {
        Agendamento agendamento = agendamentoService.criar(
                req.getClienteId(), req.getProfissionalId(), req.getServicoId(),
                req.getData(), req.getHoraInicio(), req.getObservacao());
        return ResponseEntity.ok(agendamento);
    }

    /** Para qualquer mudança de status QUE NÃO SEJA conclusão (agendado/confirmado/em atendimento/cancelado/faltou). */
    @PatchMapping("/{id}/status")
    public Agendamento atualizarStatus(@PathVariable Long id, @RequestParam StatusAgendamento status) {
        return agendamentoService.atualizarStatus(id, status);
    }

    /** Conclui o agendamento e já gera Receita + Comissão automaticamente. */
    @PatchMapping("/{id}/concluir")
    public Agendamento concluir(@PathVariable Long id, @RequestParam FormaPagamento formaPagamento) {
        return agendamentoService.concluir(id, formaPagamento);
    }

    @Data
    public static class NovoAgendamentoRequest {
        private Long clienteId;
        private Long profissionalId;
        private Long servicoId;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate data;
        @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
        private LocalTime horaInicio;
        private String observacao;
    }
}
