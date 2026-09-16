package com.example.demo.service;

import com.example.demo.enums.Papel;
import com.example.demo.enums.StatusAssinaturaSaas;
import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.exception.RegraDeNegocioException;
import com.example.demo.model.Barbearia;
import com.example.demo.model.Profissional;
import com.example.demo.model.Usuario;
import com.example.demo.repository.BarbeariaRepository;
import com.example.demo.repository.ProfissionalRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfissionalService {

    private final ProfissionalRepository profissionalRepository;
    private final BarbeariaRepository barbeariaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

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

        // O limite só vale depois que a barbearia assina um plano de verdade — durante o
        // período de teste grátis ela pode explorar o sistema sem essa restrição.
        Integer limite = barbearia.getPlanoSaas().getLimiteProfissionais();
        if (barbearia.getStatusAssinaturaSaas() == StatusAssinaturaSaas.ATIVA
                && limite != null && profissionalRepository.countByBarbeariaId(barbeariaId) >= limite) {
            throw new RegraDeNegocioException(
                    "Seu plano atual (" + barbearia.getPlanoSaas() + ") permite no máximo " + limite +
                    (limite == 1 ? " profissional." : " profissionais.") +
                    " Faça upgrade do plano para cadastrar mais.");
        }

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

    /**
     * Cria o login desse profissional. Ele passa a poder entrar no painel, mas
     * enxergando só a própria agenda e comissão — nunca a barbearia inteira.
     */
    @Transactional
    public void criarAcesso(Long profissionalId, String email, String senha) {
        Profissional profissional = buscar(profissionalId);

        if (usuarioRepository.existsByEmail(email)) {
            throw new RegraDeNegocioException("Já existe um usuário com este e-mail.");
        }
        if (usuarioRepository.existsByProfissionalId(profissionalId)) {
            throw new RegraDeNegocioException("Este profissional já tem um acesso ao painel.");
        }

        Usuario usuario = Usuario.builder()
                .nome(profissional.getNome())
                .email(email)
                .senhaHash(passwordEncoder.encode(senha))
                .papel(Papel.PROFISSIONAL)
                .barbearia(profissional.getBarbearia())
                .profissional(profissional)
                .build();

        usuarioRepository.save(usuario);
    }
}
