package com.example.demo.repository;

import com.example.demo.model.Profissional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {
    List<Profissional> findAllByBarbeariaId(Long barbeariaId);
    Optional<Profissional> findByIdAndBarbeariaId(Long id, Long barbeariaId);
    long countByBarbeariaId(Long barbeariaId);
}
