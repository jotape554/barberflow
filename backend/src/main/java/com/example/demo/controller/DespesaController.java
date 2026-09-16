package com.example.demo.controller;

import com.example.demo.model.Despesa;
import com.example.demo.service.DespesaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/despesas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR','GERENTE')")
public class DespesaController {

    private final DespesaService despesaService;

    @GetMapping
    public List<Despesa> listar() {
        return despesaService.listar();
    }

    @GetMapping("/{id}")
    public Despesa buscar(@PathVariable Long id) {
        return despesaService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Despesa> criar(@Valid @RequestBody Despesa despesa) {
        return ResponseEntity.ok(despesaService.criar(despesa));
    }

    @PutMapping("/{id}")
    public Despesa atualizar(@PathVariable Long id, @Valid @RequestBody Despesa despesa) {
        return despesaService.atualizar(id, despesa);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        despesaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
