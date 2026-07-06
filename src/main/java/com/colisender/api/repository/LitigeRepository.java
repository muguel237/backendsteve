package com.colisender.api.repository;

import com.colisender.api.model.Litige;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LitigeRepository extends JpaRepository<Litige, UUID> {
    List<Litige> findByStatutLitige(String statut);
    List<Litige> findByIdAdministrateur(UUID idAdmin);
    long countByStatutLitige(String statut);
    List<Litige> findByIdColisIn(java.util.Collection<UUID> idColis);
    boolean existsByIdColis(UUID idColis);
}
