package com.example.demo.service;

import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.model.Barbearia;
import com.example.demo.model.Servico;
import com.example.demo.repository.BarbeariaRepository;
import com.example.demo.repository.ServicoRepository;
import com.example.demo.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServicoService {

    private final ServicoRepository servicoRepository;
    private final BarbeariaRepository barbeariaRepository;

    public List<Servico> listar() {
        return servicoRepository.findAllByBarbeariaId(SecurityUtils.barbeariaAtualId());
    }

    public Servico buscar(Long id) {
        return servicoRepository.findByIdAndBarbeariaId(id, SecurityUtils.barbeariaAtualId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado"));
    }

    public Servico criar(Servico servico) {
        Long barbeariaId = SecurityUtils.barbeariaAtualId();
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));
        servico.setId(null);
        servico.setBarbearia(barbearia);
        return servicoRepository.save(servico);
    }

    public Servico atualizar(Long id, Servico dados) {
        Servico existente = buscar(id);
        existente.setNome(dados.getNome());
        existente.setDescricao(dados.getDescricao());
        existente.setPreco(dados.getPreco());
        existente.setDuracaoMinutos(dados.getDuracaoMinutos());
        existente.setAtivo(dados.isAtivo());
        return servicoRepository.save(existente);
    }

    public void excluir(Long id) {
        servicoRepository.delete(buscar(id));
    }
}
