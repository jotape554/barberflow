package com.example.demo.repository;

import com.example.demo.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServicoRepository extends JpaRepository<Servico, Long> {
    List<Servico> findAllByBarbeariaId(Long barbeariaId);
    List<Servico> findAllByBarbeariaIdAndAtivoTrue(Long barbeariaId);
    Optional<Servico> findByIdAndBarbeariaId(Long id, Long barbeariaId);
}
