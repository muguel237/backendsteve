package com.colisender.api.service;

import com.colisender.api.dto.*;
import com.colisender.api.model.*;
import com.colisender.api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class AdminService {

    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private VerificationIdentiteRepository verificationIdentiteRepository;
    @Autowired private ColisRepository colisRepository;
    @Autowired private PaiementRepository paiementRepository;
    @Autowired private LitigeRepository litigeRepository;
    @Autowired private NotificationRepository notificationRepository;

    // ── Vérifier qu'un utilisateur est bien admin ─────────────────────────────
    public void verifierAdmin(UUID idUtilisateur) {
        // Pour l'instant : tout utilisateur ACTIF peut être admin
        // À terme, vérifier la table administrateur
        Utilisateur u = utilisateurRepository.findById(idUtilisateur)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié."));
        if (u.getStatusCompte() != StatutCompteEnum.ACTIF) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé.");
        }
    }

    public void envoyerBroadcast(String contenu) {

    List<Utilisateur> users = utilisateurRepository.findAll();

    List<Notification> notifications = new ArrayList<>();

    for (Utilisateur u : users) {

        Notification n = new Notification(
            u.getIdUtilisateur(),
            contenu,
            null,
            "BROADCAST"
        );

        notifications.add(n);
    }

    notificationRepository.saveAll(notifications);
}
    // ── Statistiques du tableau de bord ──────────────────────────────────────
    public AdminStatsDto getStats() {
        AdminStatsDto dto = new AdminStatsDto();
        List<Utilisateur> tous = utilisateurRepository.findAll();
        dto.setTotalUtilisateurs(tous.size());
        dto.setUtilisateursActifs(tous.stream().filter(u -> u.getStatusCompte() == StatutCompteEnum.ACTIF).count());
        dto.setUtilisateursSuspendus(tous.stream().filter(u -> u.getStatusCompte() == StatutCompteEnum.SUSPENDU).count());
        dto.setUtilisateursEnAttente(tous.stream().filter(u -> u.getStatusCompte() == StatutCompteEnum.EN_ATTENTE).count());

        List<com.colisender.api.model.Colis> tousColis = colisRepository.findAll();
        dto.setTotalColis(tousColis.size());
        dto.setColisEnCours(tousColis.stream().filter(c -> "EN_COURS".equals(c.getStatutColis())).count());
        dto.setColisLivres(tousColis.stream().filter(c -> "TERMINE".equals(c.getStatutColis())).count());

        List<Paiement> tousPaiements = paiementRepository.findAll();
        dto.setTotalPaiements(tousPaiements.size());
        dto.setMontantTotal(tousPaiements.stream()
            .filter(p -> "LIBERE".equals(p.getStatutPaiement()))
            .mapToDouble(p -> p.getMontant().doubleValue()).sum());

        dto.setVerificationsEnAttente(verificationIdentiteRepository.countByStatutVerification(StatutVerifEnum.EN_ATTENTE));
        dto.setLitigesOuverts(litigeRepository.countByStatutLitige("OUVERT"));
        dto.setLitigesEnCours(litigeRepository.countByStatutLitige("EN_COURS"));
        return dto;
    }

    // ── Liste de tous les utilisateurs enrichie ────────────────────────────────
    public List<UtilisateurAdminDto> getTousUtilisateurs() {
        List<Utilisateur> liste = utilisateurRepository.findAll();
        List<UtilisateurAdminDto> result = new ArrayList<>();
        for (Utilisateur u : liste) {
            UtilisateurAdminDto dto = toUtilisateurDto(u);
            result.add(dto);
        }
        return result;
    }

    // ── Suspendre / Réactiver un compte ──────────────────────────────────────
    public UtilisateurAdminDto changerStatutCompte(UUID idUtilisateur, String nouveauStatut) {
        Utilisateur u = utilisateurRepository.findById(idUtilisateur)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        u.setStatusCompte(StatutCompteEnum.valueOf(nouveauStatut));
        utilisateurRepository.save(u);

        // Notifier l'utilisateur
        String message = "SUSPENDU".equals(nouveauStatut)
            ? "Votre compte a été suspendu. Contactez le support pour plus d'informations."
            : "Votre compte a été réactivé. Vous pouvez maintenant vous connecter.";
        Notification notif = new Notification(u.getIdUtilisateur(), message, null, "STATUT_COMPTE");
        notificationRepository.save(notif);

        return toUtilisateurDto(u);
    }

    // ── Vérifications d'identité en attente ───────────────────────────────────
    public List<Map<String, Object>> getVerificationsEnAttente() {
        List<VerificationIdentite> liste = verificationIdentiteRepository.findByStatutVerification(StatutVerifEnum.EN_ATTENTE);
        List<Map<String, Object>> result = new ArrayList<>();
        for (VerificationIdentite v : liste) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("idVerification", v.getIdVerification());
            item.put("idUtilisateur", v.getIdUtilisateur());
            item.put("photoRectoCNI", v.getPhotoRectoCNI());
            item.put("photoVersoCNI", v.getPhotoVersoCNI());
            item.put("photoSelfie", v.getPhotoSelfie());
            item.put("scoreCorrespondance", v.getScoreCorrespondance());
            item.put("dateSoumission", v.getDateSoumission());
            item.put("statutVerification", v.getStatutVerification());

            utilisateurRepository.findById(v.getIdUtilisateur()).ifPresent(u -> {
                item.put("nomUtilisateur", u.getPrenom() + " " + u.getNom());
                item.put("email", u.getEmail());
                item.put("numeroPrincipal", u.getNumeroPrincipal());
            });
            result.add(item);
        }
        return result;
    }

    // ── Valider ou refuser une vérification d'identité ────────────────────────
    public Map<String, Object> traiterVerification(UUID idVerification, String decision, UUID idAdmin) {
        VerificationIdentite v = verificationIdentiteRepository.findById(idVerification)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vérification introuvable."));

        if ("VALIDE".equals(decision)) {
            v.setStatutVerification(StatutVerifEnum.VALIDE);
            v.setDateValidation(java.time.LocalDateTime.now());

            // Notifier l'utilisateur
            Notification n = new Notification(v.getIdUtilisateur(),
                "Votre identité a été vérifiée avec succès. Vous avez maintenant accès à toutes les fonctionnalités.", null, "VERIF_IDENTITE");
            notificationRepository.save(n);

        } else if ("REFUSE".equals(decision)) {
            v.setStatutVerification(StatutVerifEnum.REFUSE);
            Notification n = new Notification(v.getIdUtilisateur(),
                "Votre demande de vérification d'identité a été refusée. Veuillez soumettre des documents plus lisibles.", null, "VERIF_IDENTITE");
            notificationRepository.save(n);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Décision invalide : " + decision);
        }

        verificationIdentiteRepository.save(v);
        return Map.of("statut", v.getStatutVerification().toString(), "idVerification", idVerification.toString());
    }

    // ── Litiges ──────────────────────────────────────────────────────────────
    public List<LitigeDto> getTousLitiges() {
        List<Litige> liste = litigeRepository.findAll();
        List<LitigeDto> result = new ArrayList<>();
        for (Litige l : liste) result.add(toLitigeDto(l));
        return result;
    }

    public LitigeDto traiterLitige(UUID idLitige, String nouveauStatut, UUID idAdmin) {
        Litige l = litigeRepository.findById(idLitige)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Litige introuvable."));
        l.setStatutLitige(nouveauStatut);
        l.setIdAdministrateur(idAdmin);
        litigeRepository.save(l);

        // Notifier l'expéditeur du colis
        colisRepository.findById(l.getIdColis()).ifPresent(c -> {
            String msg = "RESOLU".equals(nouveauStatut)
                ? "Le litige concernant votre colis " + c.getVilleDepart() + " → " + c.getVilleArrive() + " a été résolu."
                : "Le statut du litige concernant votre colis a été mis à jour : " + nouveauStatut + ".";
            Notification n = new Notification(c.getIdUtilisateur(), msg, c.getIdColis(), "LITIGE");
            notificationRepository.save(n);
        });

        return toLitigeDto(l);
    }

    // ── Supprimer un utilisateur ───────────────────────────────────────────────
    public void supprimerUtilisateur(UUID idUtilisateur) {
        if (!utilisateurRepository.existsById(idUtilisateur)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable.");
        }
        utilisateurRepository.deleteById(idUtilisateur);
    }

    // ── Liste de tous les colis ────────────────────────────────────────────────
    public List<com.colisender.api.model.Colis> getTousColis() {
        return colisRepository.findAll();
    }

    // ── Liste de tous les paiements ────────────────────────────────────────────
    public List<Paiement> getTousPaiements() {
        return paiementRepository.findAll();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private UtilisateurAdminDto toUtilisateurDto(Utilisateur u) {
        UtilisateurAdminDto dto = new UtilisateurAdminDto();
        dto.setIdUtilisateur(u.getIdUtilisateur());
        dto.setNom(u.getNom());
        dto.setPrenom(u.getPrenom());
        dto.setEmail(u.getEmail());
        dto.setNumeroPrincipal(u.getNumeroPrincipal());
        dto.setPhotoProfile(u.getPhotoProfil());
        dto.setStatusCompte(u.getStatusCompte().toString());
        dto.setDateCreation(u.getDateCreation());

        verificationIdentiteRepository
            .findTopByIdUtilisateurOrderByDateSoumissionDesc(u.getIdUtilisateur())
            .ifPresent(v -> {
                dto.setStatutVerification(v.getStatutVerification().toString());
                dto.setScoreCorrespondance(v.getScoreCorrespondance());
            });
        return dto;
    }

    private LitigeDto toLitigeDto(Litige l) {
        LitigeDto dto = new LitigeDto();
        dto.setIdLitige(l.getIdLitige());
        dto.setIdColis(l.getIdColis());
        dto.setDescription(l.getDescription());
        dto.setStatutLitige(l.getStatutLitige());
        dto.setDateCreation(l.getDateCreation());
        dto.setIdAdministrateur(l.getIdAdministrateur());

        colisRepository.findById(l.getIdColis()).ifPresent(c -> {
            dto.setDescriptionColis(c.getDescription());
            dto.setVilleDepart(c.getVilleDepart());
            dto.setVilleArrive(c.getVilleArrive());
            utilisateurRepository.findById(c.getIdUtilisateur())
                .ifPresent(u -> dto.setNomExpediteur(u.getPrenom() + " " + u.getNom()));
            if (c.getIdTransporteur() != null) {
                utilisateurRepository.findById(c.getIdTransporteur())
                    .ifPresent(u -> dto.setNomTransporteur(u.getPrenom() + " " + u.getNom()));
            }
        });
        return dto;
    }
}
