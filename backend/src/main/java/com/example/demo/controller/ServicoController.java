package com.example.demo.controller;

import com.example.demo.model.Servico;
import com.example.demo.service.ServicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicos")
@RequiredArgsConstructor
public class ServicoController {

    private final ServicoService servicoService;

    @GetMapping
    public List<Servico> listar() {
        return servicoService.listar();
    }

    @GetMapping("/{id}")
    public Servico buscar(@PathVariable Long id) {
        return servicoService.buscar(id);
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR','GERENTE')")
    @PostMapping
    public ResponseEntity<Servico> criar(@Valid @RequestBody Servico servico) {
        return ResponseEntity.ok(servicoService.criar(servico));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR','GERENTE')")
    @PutMapping("/{id}")
    public Servico atualizar(@PathVariable Long id, @Valid @RequestBody Servico servico) {
        return servicoService.atualizar(id, servico);
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR','GERENTE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        servicoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
