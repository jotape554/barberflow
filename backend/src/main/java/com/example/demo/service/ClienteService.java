package com.example.demo.service;

import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.model.Barbearia;
import com.example.demo.model.Cliente;
import com.example.demo.repository.BarbeariaRepository;
import com.example.demo.repository.ClienteRepository;
import com.example.demo.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final BarbeariaRepository barbeariaRepository;

    /** Lista completa, sem paginação — usada em seletores (ex.: dropdown de cliente na agenda). */
    public List<Cliente> listar() {
        return clienteRepository.findAllByBarbeariaId(SecurityUtils.barbeariaAtualId());
    }

    public Page<Cliente> listarPaginado(Pageable pageable) {
        return clienteRepository.findAllByBarbeariaId(SecurityUtils.barbeariaAtualId(), pageable);
    }

    public Cliente buscar(Long id) {
        return clienteRepository.findByIdAndBarbeariaId(id, SecurityUtils.barbeariaAtualId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
    }

    public Cliente criar(Cliente cliente) {
        Long barbeariaId = SecurityUtils.barbeariaAtualId();
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));
        cliente.setId(null);
        cliente.setBarbearia(barbearia);
        return clienteRepository.save(cliente);
    }

    public Cliente atualizar(Long id, Cliente dados) {
        Cliente existente = buscar(id);
        existente.setNome(dados.getNome());
        existente.setTelefone(dados.getTelefone());
        existente.setWhatsapp(dados.getWhatsapp());
        existente.setEmail(dados.getEmail());
        existente.setObservacoes(dados.getObservacoes());
        return clienteRepository.save(existente);
    }

    public void excluir(Long id) {
        clienteRepository.delete(buscar(id));
    }

    /** Usado pelo fluxo PÚBLICO de agendamento (sem JWT): localiza pelo telefone ou cria um novo. */
    @Transactional
    public Cliente buscarOuCriarPorTelefone(Long barbeariaId, String nome, String telefone) {
        return clienteRepository.findByTelefoneAndBarbeariaId(telefone, barbeariaId)
                .orElseGet(() -> {
                    Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                            .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));
                    Cliente novo = Cliente.builder()
                            .barbearia(barbearia)
                            .nome(nome)
                            .telefone(telefone)
                            .whatsapp(telefone)
                            .build();
                    return clienteRepository.save(novo);
                });
    }
}
