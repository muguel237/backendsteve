package com.colisender.api.repository;

import com.colisender.api.model.StatutVerifEnum;
import com.colisender.api.model.VerificationIdentite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationIdentiteRepository extends JpaRepository<VerificationIdentite, UUID> {
    Optional<VerificationIdentite> findTopByIdUtilisateurOrderByDateSoumissionDesc(UUID idUtilisateur);
    List<VerificationIdentite> findByStatutVerification(StatutVerifEnum statut);
    long countByStatutVerification(StatutVerifEnum statut);
}
