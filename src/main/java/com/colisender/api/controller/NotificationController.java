package com.colisender.api.controller;

import com.colisender.api.model.Notification;
import com.colisender.api.model.Utilisateur;
import com.colisender.api.repository.UtilisateurRepository;
import com.colisender.api.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @GetMapping
    public ResponseEntity<List<Notification>> lister(Principal principal) {
        UUID userId = getUtilisateurId(principal);
        return ResponseEntity.ok(notificationService.listerPourUtilisateur(userId));
    }

    @GetMapping("/non-lues/count")
    public ResponseEntity<Map<String, Long>> compterNonLues(Principal principal) {
        UUID userId = getUtilisateurId(principal);
        return ResponseEntity.ok(Map.of("count", notificationService.compterNonLues(userId)));
    }

    @PatchMapping("/{id}/lu")
    public ResponseEntity<Notification> marquerLu(@PathVariable UUID id, Principal principal) {
        UUID userId = getUtilisateurId(principal);
        return ResponseEntity.ok(notificationService.marquerLu(id, userId));
    }

    @PatchMapping("/lues/")
    public ResponseEntity<Void> marquerToutesLues(Principal principal) {
        UUID userId = getUtilisateurId(principal);
        notificationService.marquerToutesLues(userId);
        return ResponseEntity.noContent().build();
    }

    // ── Supprimer une notification ────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerNotification(@PathVariable UUID id, Principal principal) {
        UUID userId = getUtilisateurId(principal);
        notificationService.supprimer(id, userId);
        return ResponseEntity.noContent().build();
    }

    // ── Supprimer toutes les notifications de l'utilisateur ───────────────
    @DeleteMapping
    public ResponseEntity<Void> supprimerToutes(Principal principal) {
        UUID userId = getUtilisateurId(principal);
        notificationService.supprimerToutes(userId);
        return ResponseEntity.noContent().build();
    }

    private UUID getUtilisateurId(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        Utilisateur u = utilisateurRepository.findByEmail(principal.getName().toLowerCase().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        return u.getIdUtilisateur();
    }
}
