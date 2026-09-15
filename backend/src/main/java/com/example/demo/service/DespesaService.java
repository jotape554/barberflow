package com.example.demo.service;

import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.model.Barbearia;
import com.example.demo.model.Despesa;
import com.example.demo.repository.BarbeariaRepository;
import com.example.demo.repository.DespesaRepository;
import com.example.demo.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DespesaService {

    private final DespesaRepository despesaRepository;
    private final BarbeariaRepository barbeariaRepository;

    public List<Despesa> listar() {
        return despesaRepository.findAllByBarbeariaId(SecurityUtils.barbeariaAtualId());
    }

    public Despesa buscar(Long id) {
        return despesaRepository.findByIdAndBarbeariaId(id, SecurityUtils.barbeariaAtualId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Despesa não encontrada"));
    }

    public Despesa criar(Despesa despesa) {
        Long barbeariaId = SecurityUtils.barbeariaAtualId();
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));
        despesa.setId(null);
        despesa.setBarbearia(barbearia);
        return despesaRepository.save(despesa);
    }

    public Despesa atualizar(Long id, Despesa dados) {
        Despesa existente = buscar(id);
        existente.setDescricao(dados.getDescricao());
        existente.setCategoria(dados.getCategoria());
        existente.setValor(dados.getValor());
        existente.setData(dados.getData());
        existente.setObservacao(dados.getObservacao());
        return despesaRepository.save(existente);
    }

    public void excluir(Long id) {
        despesaRepository.delete(buscar(id));
    }
}
