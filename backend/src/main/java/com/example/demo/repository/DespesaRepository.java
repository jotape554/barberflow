package com.example.demo.repository;

import com.example.demo.model.Despesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {
    List<Despesa> findAllByBarbeariaId(Long barbeariaId);
    List<Despesa> findAllByBarbeariaIdAndDataBetween(Long barbeariaId, LocalDate inicio, LocalDate fim);
    Optional<Despesa> findByIdAndBarbeariaId(Long id, Long barbeariaId);
}
