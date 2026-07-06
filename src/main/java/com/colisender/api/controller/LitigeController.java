package com.colisender.api.controller;

import com.colisender.api.dto.LitigeDto;
import com.colisender.api.model.Colis;
import com.colisender.api.model.Litige;
import com.colisender.api.model.Utilisateur;
import com.colisender.api.repository.ColisRepository;
import com.colisender.api.repository.LitigeRepository;
import com.colisender.api.repository.UtilisateurRepository;
import com.colisender.api.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Endpoints utilisateur pour les litiges.
 * La gestion admin reste dans AdminController.
 */
@RestController
@RequestMapping("/api/litiges")
public class LitigeController {

    @Autowired private LitigeRepository litigeRepository;
    @Autowired private ColisRepository  colisRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private NotificationService notificationService;

    // ── Récupérer l'utilisateur courant ─────────────────────────────────────
    private Utilisateur getUser(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return utilisateurRepository.findByEmail(principal.getName().toLowerCase().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
    }

    // ── POST /api/litiges — créer un litige ─────────────────────────────────
    // Body : { "idColis": "uuid", "description": "..." }
    @PostMapping
    public ResponseEntity<Map<String, Object>> creerLitige(
            @RequestBody Map<String, String> body,
            Principal principal) {

        Utilisateur user = getUser(principal);

        String idColisStr = body.get("idColis");
        String description = body.get("description");

        if (description == null || description.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La description est obligatoire.");

        // Vérifier que le colis appartient bien à l'utilisateur (expéditeur ou transporteur)
        Colis colis = null;
        if (idColisStr != null && !idColisStr.isBlank()) {
            try {
                UUID idColis = UUID.fromString(idColisStr);
                colis = colisRepository.findById(idColis)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis introuvable."));
                boolean estConcerne = user.getIdUtilisateur().equals(colis.getIdUtilisateur())
                    || user.getIdUtilisateur().equals(colis.getIdTransporteur());
                if (!estConcerne)
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas concerné par ce colis.");
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID colis invalide.");
            }
        }

        Litige litige = new Litige();
        litige.setDescription(description.trim());
        litige.setStatutLitige("OUVERT");
        if (colis != null) litige.setIdColis(colis.getIdColis());
        litigeRepository.save(litige);

        // Notifier les admins via broadcast interne
        try {
            notificationService.broadcastToAll(
                "Nouveau litige soumis par " + user.getPrenom() + " " + user.getNom()
                + (colis != null ? " pour le colis " + colis.getVilleDepart() + " → " + colis.getVilleArrive() : "")
                + " : " + description.trim().substring(0, Math.min(80, description.trim().length())) + "...",
                "LITIGE"
            );
        } catch (Exception ignored) {}

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("idLitige", litige.getIdLitige());
        resp.put("statutLitige", litige.getStatutLitige());
        resp.put("message", "Votre litige a bien été soumis. Un administrateur va le traiter.");
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    // ── GET /api/litiges/mes-litiges — litiges de l'utilisateur courant ─────
    @GetMapping("/mes-litiges")
    public ResponseEntity<List<Map<String, Object>>> getMesLitiges(Principal principal) {
        Utilisateur user = getUser(principal);

        // Tous les colis où l'utilisateur est expéditeur ou transporteur
        List<UUID> idColis = colisRepository
            .findByIdUtilisateur(user.getIdUtilisateur())
            .stream().map(Colis::getIdColis).collect(Collectors.toList());
        List<UUID> idColisTransporteur = colisRepository
            .findByIdTransporteur(user.getIdUtilisateur())
            .stream().map(Colis::getIdColis).collect(Collectors.toList());
        idColis.addAll(idColisTransporteur);

        List<Litige> litiges = idColis.isEmpty()
            ? new ArrayList<>()
            : litigeRepository.findByIdColisIn(idColis);

        List<Map<String, Object>> result = litiges.stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("idLitige", l.getIdLitige());
            m.put("description", l.getDescription());
            m.put("statutLitige", l.getStatutLitige());
            m.put("dateCreation", l.getDateCreation());
            if (l.getIdColis() != null) {
                colisRepository.findById(l.getIdColis()).ifPresent(c -> {
                    m.put("idColis", c.getIdColis());
                    m.put("villeDepart", c.getVilleDepart());
                    m.put("villeArrive", c.getVilleArrive());
                });
            }
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ── GET /api/litiges — tous les litiges (public, pour affichage communauté) ──
    // N'affiche que les infos non-sensibles
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getTousLitiges() {
        List<Litige> litiges = litigeRepository.findAll();
        List<Map<String, Object>> result = litiges.stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("idLitige", l.getIdLitige());
            m.put("description", l.getDescription());
            m.put("statutLitige", l.getStatutLitige());
            m.put("dateCreation", l.getDateCreation());
            if (l.getIdColis() != null) {
                colisRepository.findById(l.getIdColis()).ifPresent(c -> {
                    m.put("villeDepart", c.getVilleDepart());
                    m.put("villeArrive", c.getVilleArrive());
                });
            }
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}
