package com.example.demo.repository;

import com.example.demo.model.Agendamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query(value = FETCH_RELACOES + "where a.barbearia.id = :barbeariaId",
            countQuery = "select count(a) from Agendamento a where a.barbearia.id = :barbeariaId")
    Page<Agendamento> findAllByBarbeariaId(Long barbeariaId, Pageable pageable);

    @Query(FETCH_RELACOES + "where a.id = :id and a.barbearia.id = :barbeariaId")
    Optional<Agendamento> findByIdAndBarbeariaId(Long id, Long barbeariaId);

    @Query(FETCH_RELACOES + "where a.barbearia.id = :barbeariaId and a.data = :data")
    List<Agendamento> findAllByBarbeariaIdAndData(Long barbeariaId, LocalDate data);

    @Query(value = FETCH_RELACOES + "where a.barbearia.id = :barbeariaId and a.data = :data",
            countQuery = "select count(a) from Agendamento a where a.barbearia.id = :barbeariaId and a.data = :data")
    Page<Agendamento> findAllByBarbeariaIdAndData(Long barbeariaId, LocalDate data, Pageable pageable);

    @Query(value = FETCH_RELACOES + "where a.barbearia.id = :barbeariaId and a.profissional.id = :profissionalId",
            countQuery = "select count(a) from Agendamento a where a.barbearia.id = :barbeariaId and a.profissional.id = :profissionalId")
    Page<Agendamento> findAllByBarbeariaIdAndProfissionalId(Long barbeariaId, Long profissionalId, Pageable pageable);

    @Query(value = FETCH_RELACOES + "where a.barbearia.id = :barbeariaId and a.profissional.id = :profissionalId and a.data = :data",
            countQuery = "select count(a) from Agendamento a where a.barbearia.id = :barbeariaId and a.profissional.id = :profissionalId and a.data = :data")
    Page<Agendamento> findAllByBarbeariaIdAndProfissionalIdAndData(Long barbeariaId, Long profissionalId, LocalDate data, Pageable pageable);

    /** usado para checar conflito de horário do mesmo profissional no mesmo dia */
    List<Agendamento> findAllByProfissionalIdAndDataAndBarbeariaId(
            Long profissionalId, LocalDate data, Long barbeariaId);
}
