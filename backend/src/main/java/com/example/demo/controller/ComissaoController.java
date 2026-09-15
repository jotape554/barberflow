package com.example.demo.controller;

import com.example.demo.model.Comissao;
import com.example.demo.service.ComissaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comissoes")
@RequiredArgsConstructor
public class ComissaoController {

    private final ComissaoService comissaoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','GERENTE','PROFISSIONAL')")
    public List<Comissao> listar() {
        return comissaoService.listar();
    }

    @PatchMapping("/{id}/pagar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','GERENTE')")
    public Comissao marcarPaga(@PathVariable Long id) {
        return comissaoService.marcarPaga(id);
    }
}
