package com.example.demo.repository;

import com.example.demo.model.Comissao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComissaoRepository extends JpaRepository<Comissao, Long> {
    List<Comissao> findAllByBarbeariaId(Long barbeariaId);
    List<Comissao> findAllByBarbeariaIdAndProfissionalId(Long barbeariaId, Long profissionalId);
    Optional<Comissao> findByIdAndBarbeariaId(Long id, Long barbeariaId);
    boolean existsByAgendamentoId(Long agendamentoId);
}
