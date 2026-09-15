package com.example.demo.repository;

import com.example.demo.model.Receita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReceitaRepository extends JpaRepository<Receita, Long> {

    String FETCH_RELACOES = "select r from Receita r " +
            "left join fetch r.cliente left join fetch r.profissional left join fetch r.servico ";

    @Query(FETCH_RELACOES + "where r.barbearia.id = :barbeariaId")
    List<Receita> findAllByBarbeariaId(Long barbeariaId);

    @Query(FETCH_RELACOES + "where r.barbearia.id = :barbeariaId and r.data between :inicio and :fim")
    List<Receita> findAllByBarbeariaIdAndDataBetween(Long barbeariaId, LocalDate inicio, LocalDate fim);

    Optional<Receita> findByIdAndBarbeariaId(Long id, Long barbeariaId);
    boolean existsByAgendamentoId(Long agendamentoId);
}
