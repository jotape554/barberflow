package com.example.demo.controller;

import com.example.demo.dto.AssinaturaResponse;
import com.example.demo.dto.CheckoutResponse;
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

    /** Cria a sessão de checkout na Stripe e devolve a URL para onde o navegador deve ser redirecionado. */
    @PostMapping("/checkout")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public CheckoutResponse checkout(@RequestParam PlanoSaas plano) {
        String url = assinaturaService.criarSessaoCheckout(SecurityUtils.barbeariaAtualId(), plano);
        return new CheckoutResponse(url);
    }

    /** URL do portal da Stripe para a barbearia gerenciar forma de pagamento ou cancelar. */
    @PostMapping("/portal")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public CheckoutResponse portal() {
        String url = assinaturaService.criarSessaoPortal(SecurityUtils.barbeariaAtualId());
        return new CheckoutResponse(url);
    }
}
