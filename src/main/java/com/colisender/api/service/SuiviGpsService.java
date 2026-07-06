package com.colisender.api.service;

import com.colisender.api.dto.PositionDto;
import com.colisender.api.model.Colis;
import com.colisender.api.model.PositionGps;
import com.colisender.api.model.Utilisateur;
import com.colisender.api.repository.ColisRepository;
import com.colisender.api.repository.PositionGpsRepository;
import com.colisender.api.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Gestion du suivi GPS en temps réel pour un colis en transport.
 *
 * RÈGLES D'ACCÈS :
 *  - Le VOYAGEUR envoie sa position  → visible par l'EXPÉDITEUR et le DESTINATAIRE
 *  - Le DESTINATAIRE envoie sa position → visible par le VOYAGEUR (pour la livraison)
 *  - L'EXPÉDITEUR ne peut qu'envoyer/voir la position du voyageur (pas la sienne)
 */
@Service
public class SuiviGpsService {

    @Autowired private PositionGpsRepository positionGpsRepository;
    @Autowired private ColisRepository colisRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private ChatService chatService;

    /**
     * Enregistre/met à jour la position GPS de l'utilisateur courant pour ce colis.
     * Seuls le voyageur et le destinataire sont autorisés à envoyer leur position.
     */
    public void enregistrerPosition(UUID idColis, UUID idUtilisateur, BigDecimal lat, BigDecimal lon) {
        Colis colis = colisRepository.findById(idColis)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis non trouvé."));

        boolean estVoyageur = colis.getIdTransporteur() != null && colis.getIdTransporteur().equals(idUtilisateur);
        UUID idDestinataire = chatService.trouverDestinataire(colis);
        boolean estDestinataire = idDestinataire != null && idDestinataire.equals(idUtilisateur);

        if (!estVoyageur && !estDestinataire) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Seul le transporteur ou le destinataire peuvent partager leur position.");
        }

        PositionGps position = positionGpsRepository.findByIdColisAndIdUtilisateur(idColis, idUtilisateur)
            .orElseGet(PositionGps::new);
        position.setIdColis(idColis);
        position.setIdUtilisateur(idUtilisateur);
        position.setLatitude(lat);
        position.setLongitude(lon);
        position.setDatePosition(java.time.LocalDateTime.now());
        positionGpsRepository.save(position);
    }

    /**
     * Retourne la position du VOYAGEUR pour ce colis.
     * Accessible par : l'expéditeur et le destinataire.
     */
    public PositionDto getPositionVoyageur(UUID idColis, UUID idUtilisateurCourant) {
        Colis colis = colisRepository.findById(idColis)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis non trouvé."));

        if (colis.getIdTransporteur() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun transporteur assigné à ce colis.");
        }

        UUID idDestinataire = chatService.trouverDestinataire(colis);
        boolean estExpediteur = colis.getIdUtilisateur().equals(idUtilisateurCourant);
        boolean estDestinataire = idDestinataire != null && idDestinataire.equals(idUtilisateurCourant);

        if (!estExpediteur && !estDestinataire) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé.");
        }

        return toDto(idColis, colis.getIdTransporteur(), "VOYAGEUR");
    }

    /**
     * Retourne la position du DESTINATAIRE pour ce colis.
     * Accessible par : le voyageur uniquement.
     */
    public PositionDto getPositionDestinataire(UUID idColis, UUID idUtilisateurCourant) {
        Colis colis = colisRepository.findById(idColis)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis non trouvé."));

        boolean estVoyageur = colis.getIdTransporteur() != null && colis.getIdTransporteur().equals(idUtilisateurCourant);
        if (!estVoyageur) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé.");
        }

        UUID idDestinataire = chatService.trouverDestinataire(colis);
        if (idDestinataire == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Le destinataire n'a pas de compte Colisender, sa position n'est pas disponible.");
        }

        return toDto(idColis, idDestinataire, "DESTINATAIRE");
    }

    /**
     * Retourne MA propre dernière position connue pour ce colis (si déjà partagée).
     * Permet au voyageur/destinataire d'afficher son propre marqueur sur la carte.
     */
    public PositionDto getMaPosition(UUID idColis, UUID idUtilisateurCourant) {
        Colis colis = colisRepository.findById(idColis)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis non trouvé."));

        boolean estVoyageur = colis.getIdTransporteur() != null && colis.getIdTransporteur().equals(idUtilisateurCourant);
        UUID idDestinataire = chatService.trouverDestinataire(colis);
        boolean estDestinataire = idDestinataire != null && idDestinataire.equals(idUtilisateurCourant);

        if (!estVoyageur && !estDestinataire) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Seul le transporteur ou le destinataire partagent leur position.");
        }

        String role = estVoyageur ? "VOYAGEUR" : "DESTINATAIRE";
        return toDto(idColis, idUtilisateurCourant, role);
    }

    private PositionDto toDto(UUID idColis, UUID idUtilisateurCible, String role) {
        PositionGps position = positionGpsRepository.findByIdColisAndIdUtilisateur(idColis, idUtilisateurCible)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Aucune position partagée pour le moment."));

        Utilisateur u = utilisateurRepository.findById(idUtilisateurCible)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé."));

        PositionDto dto = new PositionDto();
        dto.setIdUtilisateur(u.getIdUtilisateur());
        dto.setNom(u.getNom());
        dto.setPrenom(u.getPrenom());
        dto.setRole(role);
        dto.setLatitude(position.getLatitude());
        dto.setLongitude(position.getLongitude());
        dto.setDatePosition(position.getDatePosition());
        return dto;
    }
}
