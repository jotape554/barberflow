package com.example.demo.repository;

import com.example.demo.model.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    List<Agendamento> findAllByBarbeariaId(Long barbeariaId);

    Optional<Agendamento> findByIdAndBarbeariaId(Long id, Long barbeariaId);

    List<Agendamento> findAllByBarbeariaIdAndData(Long barbeariaId, LocalDate data);

    /** usado para checar conflito de horário do mesmo profissional no mesmo dia */
    List<Agendamento> findAllByProfissionalIdAndDataAndBarbeariaId(
            Long profissionalId, LocalDate data, Long barbeariaId);
}
