package com.colisender.api.repository;

import com.colisender.api.model.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaiementRepository extends JpaRepository<Paiement, UUID> {
    List<Paiement> findByIdColis(UUID idColis);
    Optional<Paiement> findByReferenceCampay(String referenceCampay);
    Optional<Paiement> findTopByIdColisOrderByDateDesc(UUID idColis);
}
