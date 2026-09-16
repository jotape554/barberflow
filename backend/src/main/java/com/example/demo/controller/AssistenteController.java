package com.example.demo.controller;

import com.example.demo.dto.PerguntaAssistenteRequest;
import com.example.demo.dto.RespostaAssistenteResponse;
import com.example.demo.service.AssistenteIaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Recurso PREMIUM — ver Recurso.ASSISTENTE_IA e RecursoGateFilter. */
@RestController
@RequestMapping("/api/assistente")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR','GERENTE')")
public class AssistenteController {

    private final AssistenteIaService assistenteIaService;

    @PostMapping("/perguntar")
    public RespostaAssistenteResponse perguntar(@Valid @RequestBody PerguntaAssistenteRequest req) {
        return new RespostaAssistenteResponse(assistenteIaService.responder(req.getPergunta()));
    }

    @GetMapping("/sugestoes")
    public List<String> sugestoes() {
        return List.of(
                "Como está minha barbearia hoje?",
                "Quanto faturei este mês?",
                "Quais serviços vendem mais?",
                "Como estão meus profissionais?",
                "Quais clientes preciso reativar?"
        );
    }
}
