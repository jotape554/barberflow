package com.example.demo.controller;

import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.model.*;
import com.example.demo.repository.BarbeariaRepository;
import com.example.demo.repository.ProfissionalRepository;
import com.example.demo.repository.ServicoRepository;
import com.example.demo.service.AgendamentoService;
import com.example.demo.service.ClienteService;
import com.example.demo.service.DisponibilidadeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Endpoints SEM autenticação — usados pela página pública de agendamento.
 * O cliente final nunca acessa o painel administrativo por aqui.
 */
@RestController
@RequestMapping("/public/barbearias")
@RequiredArgsConstructor
public class PublicController {

    private final BarbeariaRepository barbeariaRepository;
    private final ServicoRepository servicoRepository;
    private final ProfissionalRepository profissionalRepository;
    private final DisponibilidadeService disponibilidadeService;
    private final ClienteService clienteService;
    private final AgendamentoService agendamentoService;

    @GetMapping("/{slug}")
    public Barbearia buscarPorSlug(@PathVariable String slug) {
        return barbeariaRepository.findBySlug(slug)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));
    }

    @GetMapping("/{slug}/servicos")
    public List<Servico> servicosAtivos(@PathVariable String slug) {
        Barbearia barbearia = buscarPorSlug(slug);
        return servicoRepository.findAllByBarbeariaIdAndAtivoTrue(barbearia.getId());
    }

    @GetMapping("/{slug}/profissionais")
    public List<Profissional> profissionais(@PathVariable String slug) {
        Barbearia barbearia = buscarPorSlug(slug);
        return profissionalRepository.findAllByBarbeariaId(barbearia.getId())
                .stream().filter(Profissional::isAtivo).toList();
    }

    @GetMapping("/{slug}/horarios-disponiveis")
    public List<LocalTime> horariosDisponiveis(
            @PathVariable String slug,
            @RequestParam Long profissionalId,
            @RequestParam Long servicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {

        Barbearia barbearia = buscarPorSlug(slug);
        return disponibilidadeService.horariosDisponiveis(barbearia.getId(), profissionalId, servicoId, data);
    }

    /** Fluxo completo do cliente final: localiza/cria o cliente pelo telefone e cria o agendamento. */
    @PostMapping("/{slug}/agendamentos")
    public ResponseEntity<Agendamento> agendar(@PathVariable String slug, @RequestBody AgendamentoPublicoRequest req) {
        Barbearia barbearia = buscarPorSlug(slug);

        Cliente cliente = clienteService.buscarOuCriarPorTelefone(barbearia.getId(), req.getNomeCliente(), req.getTelefoneCliente());

        Agendamento agendamento = agendamentoService.criarComBarbeariaId(
                barbearia.getId(), cliente.getId(), req.getProfissionalId(), req.getServicoId(),
                req.getData(), req.getHoraInicio(), req.getObservacao());

        return ResponseEntity.ok(agendamento);
    }

    @Data
    public static class AgendamentoPublicoRequest {
        private String nomeCliente;
        private String telefoneCliente;
        private Long profissionalId;
        private Long servicoId;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate data;
        @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
        private LocalTime horaInicio;
        private String observacao;
    }
}
