package com.colisender.api.repository;

import com.colisender.api.model.Colis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ColisRepository extends JpaRepository<Colis, UUID> {
    // Spring Data générera la requête SQL automatiquement
    List<Colis> findByIdUtilisateur(UUID idUtilisateur);

    // Annonces disponibles : colis EN_ATTENTE d'autres utilisateurs
    List<Colis> findByStatutColisAndIdUtilisateurNot(String statutColis, UUID idUtilisateur);

    List<Colis> findByStatutColisAndIdUtilisateurNotAndVilleDepartContainingIgnoreCase(
            String statutColis, UUID idUtilisateur, String villeDepart);
    List<Colis> findByIdTransporteur(UUID idTransporteur);

    // Colis reçus : recherche par numéro(s) de téléphone destinataire (variantes avec/sans 237)
    List<Colis> findByTelephoneDestinataireIn(java.util.Collection<String> telephones);
}