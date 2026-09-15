package com.example.demo.controller;

import com.example.demo.model.Receita;
import com.example.demo.repository.ReceitaRepository;
import com.example.demo.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Somente leitura por enquanto: a criação acontece automaticamente ao concluir um agendamento. */
@RestController
@RequestMapping("/api/receitas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR','GERENTE')")
public class ReceitaController {

    private final ReceitaRepository receitaRepository;

    @GetMapping
    public List<Receita> listar() {
        return receitaRepository.findAllByBarbeariaId(SecurityUtils.barbeariaAtualId());
    }
}
