package com.example.demo.repository;

import com.example.demo.model.Receita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReceitaRepository extends JpaRepository<Receita, Long> {
    List<Receita> findAllByBarbeariaId(Long barbeariaId);
    List<Receita> findAllByBarbeariaIdAndDataBetween(Long barbeariaId, LocalDate inicio, LocalDate fim);
    Optional<Receita> findByIdAndBarbeariaId(Long id, Long barbeariaId);
    boolean existsByAgendamentoId(Long agendamentoId);
}
