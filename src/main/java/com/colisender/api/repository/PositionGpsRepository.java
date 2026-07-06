package com.colisender.api.repository;

import com.colisender.api.model.PositionGps;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PositionGpsRepository extends JpaRepository<PositionGps, UUID> {
    Optional<PositionGps> findByIdColisAndIdUtilisateur(UUID idColis, UUID idUtilisateur);
}
