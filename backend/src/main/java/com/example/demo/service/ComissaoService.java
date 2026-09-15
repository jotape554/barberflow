package com.example.demo.service;

import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.model.Comissao;
import com.example.demo.repository.ComissaoRepository;
import com.example.demo.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ComissaoService {

    private final ComissaoRepository comissaoRepository;

    /** Profissional só vê a própria comissão; administrador/gerente veem tudo da barbearia. */
    public List<Comissao> listar() {
        if (SecurityUtils.isProfissional()) {
            return listarPorProfissional(SecurityUtils.profissionalAtualId());
        }
        return comissaoRepository.findAllByBarbeariaId(SecurityUtils.barbeariaAtualId());
    }

    public List<Comissao> listarPorProfissional(Long profissionalId) {
        return comissaoRepository.findAllByBarbeariaIdAndProfissionalId(SecurityUtils.barbeariaAtualId(), profissionalId);
    }

    public Comissao marcarPaga(Long id) {
        Comissao comissao = comissaoRepository.findByIdAndBarbeariaId(id, SecurityUtils.barbeariaAtualId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Comissão não encontrada"));
        comissao.setPaga(true);
        return comissaoRepository.save(comissao);
    }
}
