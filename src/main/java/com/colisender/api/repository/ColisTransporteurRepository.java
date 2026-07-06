package com.colisender.api.repository;
 
import com.colisender.api.model.ColisTransporteur;
import com.colisender.api.model.ColisTransporteurId;
import com.colisender.api.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
 
import java.util.List;
import java.util.UUID;
 
public interface ColisTransporteurRepository extends JpaRepository<ColisTransporteur, ColisTransporteurId> {
 
    boolean existsByIdColisAndIdUtilisateur(UUID idColis, UUID idUtilisateur);
 
    List<ColisTransporteur> findByIdColis(UUID idColis);
 
    // Retourne les Utilisateurs (voyageurs) ayant postulé sur un colis donné
    @Query("SELECT u FROM Utilisateur u WHERE u.idUtilisateur IN " +
           "(SELECT ct.idUtilisateur FROM ColisTransporteur ct WHERE ct.idColis = :idColis)")
    List<Utilisateur> findVoyageursByColisId(@Param("idColis") UUID idColis);
}
