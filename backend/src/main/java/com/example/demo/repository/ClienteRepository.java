package com.example.demo.repository;

import com.example.demo.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    List<Cliente> findAllByBarbeariaId(Long barbeariaId);
    Optional<Cliente> findByIdAndBarbeariaId(Long id, Long barbeariaId);
    Optional<Cliente> findByTelefoneAndBarbeariaId(String telefone, Long barbeariaId);
}
