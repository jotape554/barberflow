package com.example.demo.controller;

import com.example.demo.dto.CriarAcessoRequest;
import com.example.demo.model.Profissional;
import com.example.demo.service.ProfissionalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profissionais")
@RequiredArgsConstructor
public class ProfissionalController {

    private final ProfissionalService profissionalService;

    @GetMapping
    public List<Profissional> listar() {
        return profissionalService.listar();
    }

    @GetMapping("/{id}")
    public Profissional buscar(@PathVariable Long id) {
        return profissionalService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Profissional> criar(@RequestBody Profissional profissional) {
        return ResponseEntity.ok(profissionalService.criar(profissional));
    }

    @PutMapping("/{id}")
    public Profissional atualizar(@PathVariable Long id, @RequestBody Profissional profissional) {
        return profissionalService.atualizar(id, profissional);
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR','GERENTE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        profissionalService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    /** Cria o login do profissional, restrito à própria agenda/comissão no painel. */
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','GERENTE')")
    @PostMapping("/{id}/acesso")
    public ResponseEntity<Void> criarAcesso(@PathVariable Long id, @Valid @RequestBody CriarAcessoRequest req) {
        profissionalService.criarAcesso(id, req.getEmail(), req.getSenha());
        return ResponseEntity.noContent().build();
    }
}
