package com.example.demo.controller;

import com.example.demo.dto.AssinaturaResponse;
import com.example.demo.dto.PlanoDisponivelResponse;
import com.example.demo.enums.PlanoSaas;
import com.example.demo.security.SecurityUtils;
import com.example.demo.service.AssinaturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assinatura")
@RequiredArgsConstructor
public class AssinaturaController {

    private final AssinaturaService assinaturaService;

    @GetMapping("/planos")
    public List<PlanoDisponivelResponse> planos() {
        return assinaturaService.listarPlanos();
    }

    @GetMapping
    public AssinaturaResponse status() {
        return assinaturaService.status(SecurityUtils.barbeariaAtualId());
    }

    /**
     * Registra o plano desejado pela barbearia. Isso NÃO libera acesso sozinho — nenhum
     * gateway de pagamento está integrado ainda, então o status só muda para ATIVA quando
     * uma cobrança de verdade for confirmada (ver AssinaturaService.confirmarPagamento).
     */
    @PatchMapping("/plano")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public AssinaturaResponse escolherPlano(@RequestParam PlanoSaas plano) {
        return assinaturaService.escolherPlano(SecurityUtils.barbeariaAtualId(), plano);
    }
}
