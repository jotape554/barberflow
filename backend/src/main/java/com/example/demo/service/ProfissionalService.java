package com.example.demo.service;

import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.model.Barbearia;
import com.example.demo.model.Profissional;
import com.example.demo.repository.BarbeariaRepository;
import com.example.demo.repository.ProfissionalRepository;
import com.example.demo.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfissionalService {

    private final ProfissionalRepository profissionalRepository;
    private final BarbeariaRepository barbeariaRepository;

    public List<Profissional> listar() {
        return profissionalRepository.findAllByBarbeariaId(SecurityUtils.barbeariaAtualId());
    }

    public Profissional buscar(Long id) {
        return profissionalRepository.findByIdAndBarbeariaId(id, SecurityUtils.barbeariaAtualId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional não encontrado"));
    }

    public Profissional criar(Profissional profissional) {
        Long barbeariaId = SecurityUtils.barbeariaAtualId();
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));
        profissional.setId(null);
        profissional.setBarbearia(barbearia);
        return profissionalRepository.save(profissional);
    }

    public Profissional atualizar(Long id, Profissional dados) {
        Profissional existente = buscar(id);
        existente.setNome(dados.getNome());
        existente.setFotoUrl(dados.getFotoUrl());
        existente.setTelefone(dados.getTelefone());
        existente.setEmail(dados.getEmail());
        existente.setFuncao(dados.getFuncao());
        existente.setDiasTrabalho(dados.getDiasTrabalho());
        existente.setHorarioInicio(dados.getHorarioInicio());
        existente.setHorarioFim(dados.getHorarioFim());
        existente.setPercentualComissao(dados.getPercentualComissao());
        existente.setAtivo(dados.isAtivo());
        return profissionalRepository.save(existente);
    }

    public void excluir(Long id) {
        profissionalRepository.delete(buscar(id));
    }
}
