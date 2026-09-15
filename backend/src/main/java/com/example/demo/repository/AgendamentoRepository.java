package com.example.demo.repository;

import com.example.demo.model.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    String FETCH_RELACOES = "select a from Agendamento a " +
            "join fetch a.cliente join fetch a.profissional join fetch a.servico ";

    @Query(FETCH_RELACOES + "where a.barbearia.id = :barbeariaId")
    List<Agendamento> findAllByBarbeariaId(Long barbeariaId);

    @Query(FETCH_RELACOES + "where a.id = :id and a.barbearia.id = :barbeariaId")
    Optional<Agendamento> findByIdAndBarbeariaId(Long id, Long barbeariaId);

    @Query(FETCH_RELACOES + "where a.barbearia.id = :barbeariaId and a.data = :data")
    List<Agendamento> findAllByBarbeariaIdAndData(Long barbeariaId, LocalDate data);

    @Query(FETCH_RELACOES + "where a.barbearia.id = :barbeariaId and a.profissional.id = :profissionalId")
    List<Agendamento> findAllByBarbeariaIdAndProfissionalId(Long barbeariaId, Long profissionalId);

    @Query(FETCH_RELACOES + "where a.barbearia.id = :barbeariaId and a.profissional.id = :profissionalId and a.data = :data")
    List<Agendamento> findAllByBarbeariaIdAndProfissionalIdAndData(Long barbeariaId, Long profissionalId, LocalDate data);

    /** usado para checar conflito de horário do mesmo profissional no mesmo dia */
    List<Agendamento> findAllByProfissionalIdAndDataAndBarbeariaId(
            Long profissionalId, LocalDate data, Long barbeariaId);
}
