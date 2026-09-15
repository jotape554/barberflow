package com.example.demo.repository;

import com.example.demo.model.Barbearia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BarbeariaRepository extends JpaRepository<Barbearia, Long> {
    Optional<Barbearia> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Optional<Barbearia> findByStripeCustomerId(String stripeCustomerId);
    Optional<Barbearia> findByStripeSubscriptionId(String stripeSubscriptionId);
}
