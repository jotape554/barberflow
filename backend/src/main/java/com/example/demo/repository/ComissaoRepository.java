package com.example.demo.repository;

import com.example.demo.model.Comissao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ComissaoRepository extends JpaRepository<Comissao, Long> {

    String FETCH_RELACOES = "select c from Comissao c join fetch c.profissional ";

    @Query(FETCH_RELACOES + "where c.barbearia.id = :barbeariaId")
    List<Comissao> findAllByBarbeariaId(Long barbeariaId);

    @Query(FETCH_RELACOES + "where c.barbearia.id = :barbeariaId and c.profissional.id = :profissionalId")
    List<Comissao> findAllByBarbeariaIdAndProfissionalId(Long barbeariaId, Long profissionalId);

    Optional<Comissao> findByIdAndBarbeariaId(Long id, Long barbeariaId);
    boolean existsByAgendamentoId(Long agendamentoId);
}
