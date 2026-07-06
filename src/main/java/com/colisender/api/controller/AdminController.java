package com.colisender.api.controller;

import com.colisender.api.dto.*;
import com.colisender.api.model.*;
import com.colisender.api.repository.UtilisateurRepository;
import com.colisender.api.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.colisender.api.service.NotificationService;

import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired private AdminService adminService;
    @Autowired private UtilisateurRepository utilisateurRepository;
     @Autowired private NotificationService notificationService;

    // ── Statistiques globales ────────────────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> getStats(Principal principal) {
        verifierAdmin(principal);
        return ResponseEntity.ok(adminService.getStats());
    }

    // ── Gestion des utilisateurs ─────────────────────────────────────────────
    @GetMapping("/utilisateurs")
    public ResponseEntity<List<UtilisateurAdminDto>> getUtilisateurs(Principal principal) {
        verifierAdmin(principal);
        return ResponseEntity.ok(adminService.getTousUtilisateurs());
    }
 @PostMapping("/notifications/broadcast")
    public ResponseEntity<Map<String, Object>> broadcastNotification(
            @RequestBody Map<String, String> body,
            Principal principal) {
        verifierAdmin(principal);

        String contenu = body.get("contenu");
        if (contenu == null || contenu.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Le contenu de la notification est requis."
            );
        }

        int nbNotifies = notificationService.broadcastToAll(contenu, "BROADCAST_ADMIN");

        return ResponseEntity.ok(Map.of(
            "message",    "Notification envoyée à " + nbNotifies + " utilisateur(s).",
            "nbNotifies", nbNotifies
        ));
    }

    @PatchMapping("/utilisateurs/{id}/statut")
    public ResponseEntity<UtilisateurAdminDto> changerStatut(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            Principal principal) {
        verifierAdmin(principal);
        String statut = body.get("statut");
        if (statut == null || (!statut.equals("ACTIF") && !statut.equals("SUSPENDU") && !statut.equals("EN_ATTENTE"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut invalide.");
        }
        return ResponseEntity.ok(adminService.changerStatutCompte(id, statut));
    }

    @DeleteMapping("/utilisateurs/{id}")
    public ResponseEntity<Void> supprimerUtilisateur(@PathVariable UUID id, Principal principal) {
        verifierAdmin(principal);
        adminService.supprimerUtilisateur(id);
        return ResponseEntity.noContent().build();
    }

    // ── Vérifications d'identité ─────────────────────────────────────────────
    @GetMapping("/verifications")
    public ResponseEntity<List<Map<String, Object>>> getVerifications(Principal principal) {
        verifierAdmin(principal);
        return ResponseEntity.ok(adminService.getVerificationsEnAttente());
    }

    @PatchMapping("/verifications/{id}")
    public ResponseEntity<Map<String, Object>> traiterVerification(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            Principal principal) {
        UUID adminId = getAdminId(principal);
        String decision = body.get("decision"); // VALIDE ou REFUSE
        return ResponseEntity.ok(adminService.traiterVerification(id, decision, adminId));
    }

    // ── Litiges ──────────────────────────────────────────────────────────────
    @GetMapping("/litiges")
    public ResponseEntity<List<LitigeDto>> getLitiges(Principal principal) {
        verifierAdmin(principal);
        return ResponseEntity.ok(adminService.getTousLitiges());
    }

    @PatchMapping("/litiges/{id}")
    public ResponseEntity<LitigeDto> traiterLitige(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            Principal principal) {
        UUID adminId = getAdminId(principal);
        String statut = body.get("statut"); // EN_COURS | RESOLU | FERME
        return ResponseEntity.ok(adminService.traiterLitige(id, statut, adminId));
    }

    // ── Colis ────────────────────────────────────────────────────────────────
    @GetMapping("/colis")
    public ResponseEntity<List<Colis>> getColis(Principal principal) {
        verifierAdmin(principal);
        return ResponseEntity.ok(adminService.getTousColis());
    }

    // ── Paiements ────────────────────────────────────────────────────────────
    @GetMapping("/paiements")
    public ResponseEntity<List<Paiement>> getPaiements(Principal principal) {
        verifierAdmin(principal);
        return ResponseEntity.ok(adminService.getTousPaiements());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private void verifierAdmin(Principal principal) {
        UUID id = getAdminId(principal);
        adminService.verifierAdmin(id);
    }

    private UUID getAdminId(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié.");
        return utilisateurRepository.findByEmail(principal.getName().toLowerCase().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."))
            .getIdUtilisateur();
    }
}
