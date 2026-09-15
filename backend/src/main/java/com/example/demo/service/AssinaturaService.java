package com.example.demo.service;

import com.example.demo.dto.AssinaturaResponse;
import com.example.demo.dto.PlanoDisponivelResponse;
import com.example.demo.enums.PlanoSaas;
import com.example.demo.enums.StatusAssinaturaSaas;
import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.model.Barbearia;
import com.example.demo.repository.BarbeariaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

/**
 * Centraliza a regra de acesso ao painel: nenhum gateway de pagamento está integrado ainda
 * (ver README), então por enquanto isso apenas controla o período de teste gratuito e o
 * plano desejado pela barbearia. Quando um gateway real for integrado, o webhook de
 * confirmação de pagamento deve chamar {@link #confirmarPagamento(Long)} para liberar o acesso.
 */
@Service
@RequiredArgsConstructor
public class AssinaturaService {

    public static final int DIAS_TRIAL = 14;

    private final BarbeariaRepository barbeariaRepository;

    public List<PlanoDisponivelResponse> listarPlanos() {
        return Arrays.stream(PlanoSaas.values())
                .map(p -> new PlanoDisponivelResponse(p, p.getPrecoMensal(), p.getDescricao()))
                .toList();
    }

    public AssinaturaResponse status(Long barbeariaId) {
        Barbearia barbearia = buscar(barbeariaId);
        return paraResponse(barbearia);
    }

    @Transactional
    public AssinaturaResponse escolherPlano(Long barbeariaId, PlanoSaas novoPlano) {
        Barbearia barbearia = buscar(barbeariaId);
        barbearia.setPlanoSaas(novoPlano);
        barbeariaRepository.save(barbearia);
        return paraResponse(barbearia);
    }

    /** Chamado pelo webhook do gateway de pagamento assim que uma cobrança é confirmada. */
    @Transactional
    public void confirmarPagamento(Long barbeariaId) {
        Barbearia barbearia = buscar(barbeariaId);
        barbearia.setStatusAssinaturaSaas(StatusAssinaturaSaas.ATIVA);
        barbeariaRepository.save(barbearia);
    }

    public boolean acessoLiberado(Barbearia barbearia) {
        if (barbearia.getStatusAssinaturaSaas() == StatusAssinaturaSaas.ATIVA) {
            return true;
        }
        if (barbearia.getStatusAssinaturaSaas() == StatusAssinaturaSaas.TRIAL) {
            LocalDate fimTrial = barbearia.getDataFimTrial();
            return fimTrial != null && !LocalDate.now().isAfter(fimTrial);
        }
        return false;
    }

    private AssinaturaResponse paraResponse(Barbearia barbearia) {
        Long diasRestantes = null;
        if (barbearia.getStatusAssinaturaSaas() == StatusAssinaturaSaas.TRIAL && barbearia.getDataFimTrial() != null) {
            diasRestantes = Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), barbearia.getDataFimTrial()));
        }

        PlanoSaas plano = barbearia.getPlanoSaas();
        return new AssinaturaResponse(
                plano,
                plano.getPrecoMensal(),
                plano.getDescricao(),
                barbearia.getStatusAssinaturaSaas(),
                barbearia.getDataFimTrial(),
                diasRestantes,
                acessoLiberado(barbearia)
        );
    }

    private Barbearia buscar(Long id) {
        return barbeariaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));
    }
}
