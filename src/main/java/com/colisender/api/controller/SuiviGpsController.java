package com.colisender.api.controller;

import com.colisender.api.dto.PositionDto;
import com.colisender.api.dto.PositionRequest;
import com.colisender.api.model.Colis;
import com.colisender.api.model.Utilisateur;
import com.colisender.api.repository.ColisRepository;
import com.colisender.api.repository.UtilisateurRepository;
import com.colisender.api.service.ChatService;
import com.colisender.api.service.SuiviGpsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/suivi")
public class SuiviGpsController {

    @Autowired private SuiviGpsService suiviGpsService;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private ColisRepository colisRepository;
    @Autowired private ChatService chatService;

    // ── POST /api/suivi/colis/{idColis}/position — envoyer ma position GPS ─────
    @PostMapping("/colis/{idColis}/position")
    public ResponseEntity<Void> envoyerPosition(
            @PathVariable UUID idColis,
            @RequestBody PositionRequest req,
            Principal principal) {
        UUID userId = getUtilisateurId(principal);
        if (req.getLatitude() == null || req.getLongitude() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Latitude et longitude requises.");
        }
        suiviGpsService.enregistrerPosition(idColis, userId, req.getLatitude(), req.getLongitude());
        return ResponseEntity.ok().build();
    }

    // ── GET /api/suivi/colis/{idColis}/voyageur — position du transporteur ──────
    // Accessible par : expéditeur, destinataire
    @GetMapping("/colis/{idColis}/voyageur")
    public ResponseEntity<PositionDto> positionVoyageur(@PathVariable UUID idColis, Principal principal) {
        UUID userId = getUtilisateurId(principal);
        return ResponseEntity.ok(suiviGpsService.getPositionVoyageur(idColis, userId));
    }

    // ── GET /api/suivi/colis/{idColis}/destinataire — position du destinataire ──
    // Accessible par : voyageur uniquement
    @GetMapping("/colis/{idColis}/destinataire")
    public ResponseEntity<PositionDto> positionDestinataire(@PathVariable UUID idColis, Principal principal) {
        UUID userId = getUtilisateurId(principal);
        return ResponseEntity.ok(suiviGpsService.getPositionDestinataire(idColis, userId));
    }

    // ── GET /api/suivi/colis/{idColis}/ma-position — ma propre dernière position ──
    // Accessible par : voyageur, destinataire (ceux qui partagent leur position)
    @GetMapping("/colis/{idColis}/ma-position")
    public ResponseEntity<PositionDto> maPosition(@PathVariable UUID idColis, Principal principal) {
        UUID userId = getUtilisateurId(principal);
        return ResponseEntity.ok(suiviGpsService.getMaPosition(idColis, userId));
    }

    /**
     * GET /api/suivi/colis/{idColis}/mon-role
     * Indique à quoi le frontend a accès pour ce colis :
     * EXPEDITEUR, VOYAGEUR, DESTINATAIRE ou AUCUN.
     */
    @GetMapping("/colis/{idColis}/mon-role")
    public ResponseEntity<String> monRole(@PathVariable UUID idColis, Principal principal) {
        UUID userId = getUtilisateurId(principal);
        Colis colis = colisRepository.findById(idColis)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis non trouvé."));

        if (colis.getIdUtilisateur().equals(userId)) return ResponseEntity.ok("EXPEDITEUR");
        if (colis.getIdTransporteur() != null && colis.getIdTransporteur().equals(userId)) return ResponseEntity.ok("VOYAGEUR");

        UUID idDestinataire = chatService.trouverDestinataire(colis);
        if (idDestinataire != null && idDestinataire.equals(userId)) return ResponseEntity.ok("DESTINATAIRE");

        return ResponseEntity.ok("AUCUN");
    }

    private UUID getUtilisateurId(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié.");
        Utilisateur u = utilisateurRepository.findByEmail(principal.getName().toLowerCase().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        return u.getIdUtilisateur();
    }
}
