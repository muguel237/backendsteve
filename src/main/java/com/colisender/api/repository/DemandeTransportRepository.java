package com.colisender.api.repository;

import com.colisender.api.model.DemandeTransport;
import com.colisender.api.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DemandeTransportRepository extends JpaRepository<DemandeTransport, UUID> {

    boolean existsByColisIdAndVoyageurId(UUID colisId, UUID voyageurId);

    List<DemandeTransport> findByColisId(UUID colisId);

    // Retourne directement les Utilisateurs (voyageurs) qui ont postulé sur un colis
    @Query("SELECT u FROM Utilisateur u WHERE u.idUtilisateur IN " +
           "(SELECT d.voyageurId FROM DemandeTransport d WHERE d.colisId = :colisId)")
    List<Utilisateur> findVoyageursByColisId(@Param("colisId") UUID colisId);
}
