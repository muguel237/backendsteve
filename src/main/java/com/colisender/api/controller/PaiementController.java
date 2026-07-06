package com.colisender.api.controller;
import com.colisender.api.model.*;
import com.colisender.api.repository.*;
import com.colisender.api.service.CampayService;
import com.colisender.api.service.EmailService;
import com.colisender.api.service.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/paiements")
public class PaiementController {

    @Autowired private PaiementRepository paiementRepository;
    @Autowired private ColisRepository colisRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private OtpRepository otpRepository;
    @Autowired private OtpLivraisonRepository otpLivraisonRepository;
    @Autowired private CampayService campayService;
    @Autowired private NotificationService notificationService;
    @Autowired private EmailService emailService;
    @Autowired private com.colisender.api.service.ChatService chatService;

    // ── POST /api/paiements/colis/{idColis}/initier ────────────────────────────
    @PostMapping("/colis/{idColis}/initier")
    public ResponseEntity<?> initierPaiement(
            @PathVariable UUID idColis,
            @RequestBody Map<String, String> body,
            Principal principal) {

        UUID userId = getUtilisateurId(principal);

        Colis colis = colisRepository.findById(idColis)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis non trouvé."));

        if (!colis.getIdUtilisateur().equals(userId)) {
            System.out.println("[PAIEMENT 403] userId=" + userId + " != colis.idUtilisateur=" + colis.getIdUtilisateur());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul l'expéditeur peut initier le paiement.");
        }

        String statut = colis.getStatutColis();
        if (!"EN_COURS".equals(statut) && !"EN_ATTENTE_PAIEMENT".equals(statut)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Ce colis n'est pas dans un état permettant le paiement (statut : " + statut + ").");
        }

        if (colis.getIdTransporteur() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun transporteur choisi pour ce colis.");
        }

        // Si un paiement SEQUESTRE existe déjà, le retourner
        Optional<Paiement> existant = paiementRepository.findTopByIdColisOrderByDateDesc(idColis);
        if (existant.isPresent() && "SEQUESTRE".equals(existant.get().getStatutPaiement())) {
            return ResponseEntity.ok(Map.of(
                "idPaiement", existant.get().getIdPaiement(),
                "statut", "SEQUESTRE",
                "message", "Le paiement est déjà sécurisé."
            ));
        }

        String numeroTelephone = body.get("numeroTelephone");
        String methodePaiement = body.get("methodePaiement");

        if (numeroTelephone == null || numeroTelephone.replaceAll("[^0-9]", "").length() < 9)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Numéro de téléphone invalide.");
        if (!"MTN_MOMO".equals(methodePaiement) && !"ORANGE_MONEY".equals(methodePaiement))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Méthode de paiement invalide.");

        // Créer l'enregistrement paiement
        Paiement paiement = new Paiement();
        paiement.setIdColis(idColis);
        paiement.setMontant(colis.getPrixTransport());
        paiement.setMethodePaiement(methodePaiement);
        paiement.setStatutPaiement("EN_ATTENTE");
        paiement.setNumeroTelephone(numeroTelephone);
        paiement.setIdTransporteur(colis.getIdTransporteur());
        paiement = paiementRepository.save(paiement);

        String montantStr = colis.getPrixTransport()
            .setScale(0, RoundingMode.HALF_UP).toPlainString();

        JsonNode reponse;
        try {
            reponse = campayService.initierCollecte(
                montantStr, numeroTelephone,
                "Transport colis " + colis.getVilleDepart() + " -> " + colis.getVilleArrive(),
                paiement.getIdPaiement().toString()
            );
        } catch (Exception e) {
            System.err.println("[PAIEMENT] Erreur Campay: " + e.getMessage());
            paiement.setStatutPaiement("ECHEC");
            paiementRepository.save(paiement);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Service de paiement momentanément indisponible: " + e.getMessage());
        }

        System.out.println("[PAIEMENT] Réponse Campay: " + reponse);

        if (reponse != null && reponse.has("reference") && !reponse.get("reference").isNull()) {
            paiement.setReferenceCampay(reponse.get("reference").asText());
            paiementRepository.save(paiement);
        } else {
            paiement.setStatutPaiement("ECHEC");
            paiementRepository.save(paiement);
            String detail = reponse != null && reponse.has("message")
                ? reponse.get("message").asText()
                : (reponse != null ? reponse.toString() : "Réponse vide");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Campay a refusé la demande: " + detail);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "idPaiement", paiement.getIdPaiement(),
            "reference", paiement.getReferenceCampay(),
            "statut", paiement.getStatutPaiement(),
            "message", "Validez la notification sur votre téléphone (" + methodePaiement.replace("_", " ") + ")."
        ));
    }

    // ── GET /api/paiements/{idPaiement}/statut ─────────────────────────────────
    @GetMapping("/{idPaiement}/statut")
    public ResponseEntity<?> verifierStatut(@PathVariable UUID idPaiement, Principal principal) {
        UUID userId = getUtilisateurId(principal);

        Paiement paiement = paiementRepository.findById(idPaiement)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paiement non trouvé."));

        Colis colis = colisRepository.findById(paiement.getIdColis())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis non trouvé."));

        boolean isExpediteur = colis.getIdUtilisateur().equals(userId);
        boolean isTransporteur = paiement.getIdTransporteur() != null && paiement.getIdTransporteur().equals(userId);
        if (!isExpediteur && !isTransporteur)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé.");

        // Déjà traité → retour direct
        if ("SEQUESTRE".equals(paiement.getStatutPaiement()) || "LIBERE".equals(paiement.getStatutPaiement()))
            return ResponseEntity.ok(Map.of("statut", paiement.getStatutPaiement()));
        if ("ECHEC".equals(paiement.getStatutPaiement()) || paiement.getReferenceCampay() == null)
            return ResponseEntity.ok(Map.of("statut", paiement.getStatutPaiement()));

        // Interroger Campay
        JsonNode reponse;
        try {
            reponse = campayService.verifierStatut(paiement.getReferenceCampay());
        } catch (Exception e) {
            System.err.println("[STATUT] Erreur Campay: " + e.getMessage());
            return ResponseEntity.ok(Map.of("statut", "EN_ATTENTE", "detail", e.getMessage()));
        }

        String statutCampay = reponse != null && reponse.has("status")
            ? reponse.get("status").asText() : "PENDING";
        System.out.println("[STATUT] Campay=" + statutCampay);

        switch (statutCampay) {
            case "SUCCESSFUL":
                paiement.setStatutPaiement("SEQUESTRE");
                paiementRepository.save(paiement);

                // ── Générer et envoyer OTP au destinataire ─────────────────
                genererEtEnvoyerOtpDestinataire(colis);

                // ── Ouvrir la conversation (expéditeur, transporteur, destinataire) ──
                try {
                    chatService.creerConversationsPourColis(colis.getIdColis());
                } catch (Exception e) {
                    System.err.println("[CHAT] Impossible de créer la conversation : " + e.getMessage());
                }

                // ── Notifier le transporteur ───────────────────────────────
                notificationService.creer(
                    paiement.getIdTransporteur(),
                    "Paiement reçu pour le colis " + colis.getVilleDepart() + " → " + colis.getVilleArrive() +
                        ". Vous pouvez désormais discuter avec l'expéditeur.",
                    colis.getIdColis(), "PAIEMENT_EFFECTUE"
                );
                break;
            case "FAILED":
            case "CANCELLED":
                paiement.setStatutPaiement("ECHEC");
                paiementRepository.save(paiement);
                break;
        }

        return ResponseEntity.ok(Map.of("statut", paiement.getStatutPaiement()));
    }

    // ── POST /api/paiements/colis/{idColis}/valider-otp ───────────────────────
    // Le voyageur saisit l'OTP du destinataire → libère les 80% sur son compte
    @PostMapping("/colis/{idColis}/valider-otp")
    public ResponseEntity<?> validerOtp(
            @PathVariable UUID idColis,
            @RequestBody Map<String, String> body,
            Principal principal) {

        UUID userId = getUtilisateurId(principal);

        Colis colis = colisRepository.findById(idColis)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis non trouvé."));

        // Seul le transporteur peut valider l'OTP
        if (!userId.equals(colis.getIdTransporteur()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul le transporteur peut valider l'OTP.");

        String otpSaisi = body.get("otp");
        if (otpSaisi == null || otpSaisi.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP requis.");

        // Bloquer si le paiement est déjà libéré (double appel, attaque)
        Paiement paiementExist = paiementRepository.findTopByIdColisOrderByDateDesc(idColis)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paiement non trouvé."));
        if ("LIBERE".equals(paiementExist.getStatutPaiement()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Ce paiement a déjà été libéré. La livraison est confirmée.");

        // Chercher l'OTP actif pour ce colis (version robuste : tolère et nettoie les doublons)
        OtpLivraison otp = getOtpLivraisonActif(idColis);
        if (otp == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun OTP actif pour ce colis.");

        if (!otp.getCodeOtp().equals(otpSaisi.trim()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP incorrect.");

        if (otp.getDateExpiration().isBefore(java.time.LocalDateTime.now())) {
            otp.setStatut(StatutOtpEnum.EXPIRE);
            otpLivraisonRepository.save(otp);
            System.err.println("[OTP] OTP expiré pour colis " + idColis
                + " — expiration: " + otp.getDateExpiration()
                + " — maintenant: " + java.time.LocalDateTime.now());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP expiré.");
        }

        // Marquer OTP utilisé : reporté après la confirmation du virement Campay
        // pour ne jamais "brûler" le code si le transfert échoue.

        // Récupérer le paiement SEQUESTRE
        Paiement paiement = paiementRepository.findTopByIdColisOrderByDateDesc(idColis)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paiement non trouvé."));

        if (!"SEQUESTRE".equals(paiement.getStatutPaiement()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le paiement n'est pas en séquestre.");

        // Calculer 80% du montant
        BigDecimal montant80 = paiement.getMontant()
            .multiply(new BigDecimal("0.80"))
            .setScale(0, RoundingMode.HALF_UP);

        // Récupérer le numéro principal du transporteur
        Utilisateur transporteur = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transporteur non trouvé."));

        String numeroTransporteur = transporteur.getNumeroPrincipal();
        if (numeroTransporteur == null || numeroTransporteur.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Le transporteur n'a pas de numéro principal enregistré.");

        // Virer les 80% via Campay et vérifier que le virement est bien accepté
        try {
            JsonNode resp = campayService.initierTransfert(
                montant80.toPlainString(),
                numeroTransporteur,
                "Paiement livraison colis " + colis.getVilleDepart() + " → " + colis.getVilleArrive(),
                paiement.getIdPaiement().toString() + "_libere"
            );
            System.out.println("[LIBERE] Campay withdraw réponse complète: " + resp);

            // Campay /withdraw/ peut retourner "reference", "withdraw_code" ou juste un statut
            // On considère le virement accepté si :
            //   - "reference" est présent, OU
            //   - "withdraw_code" est présent, OU
            //   - "status" vaut "SUCCESSFUL" ou "PENDING"
            //   - ET pas de champ "error" / "detail" indiquant un rejet
            boolean virementsAccepte = false;
            String refCampay = "";

            if (resp != null) {
                if (resp.has("reference") && !resp.get("reference").isNull() && !resp.get("reference").asText().isBlank()) {
                    virementsAccepte = true;
                    refCampay = resp.get("reference").asText();
                } else if (resp.has("withdraw_code") && !resp.get("withdraw_code").isNull()) {
                    virementsAccepte = true;
                    refCampay = resp.get("withdraw_code").asText();
                } else if (resp.has("status")) {
                    String st = resp.get("status").asText();
                    virementsAccepte = "SUCCESSFUL".equalsIgnoreCase(st) || "PENDING".equalsIgnoreCase(st);
                    refCampay = st;
                }
            }

            if (!virementsAccepte) {
                String detail = resp != null && resp.has("detail")
                    ? resp.get("detail").asText()
                    : (resp != null && resp.has("message")
                        ? resp.get("message").asText()
                        : (resp != null ? resp.toString() : "Réponse vide"));
                System.err.println("[LIBERE] Campay a rejeté le virement: " + detail);
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Le virement a été refusé par Campay: " + detail +
                    ". Contactez le support avec la référence paiement : " + paiement.getIdPaiement());
            }

            System.out.println("[LIBERE] Virement accepté par Campay. Ref: " + refCampay);

        } catch (ResponseStatusException rse) {
            // Propager les erreurs métier déjà formatées
            throw rse;
        } catch (Exception e) {
            System.err.println("[LIBERE] Erreur Campay withdraw: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Erreur lors du versement: " + e.getMessage() +
                ". Contactez le support avec la référence paiement : " + paiement.getIdPaiement());
        }

        // Virement confirmé : on peut maintenant marquer l'OTP utilisé et mettre à jour paiement + colis
        otp.setStatut(StatutOtpEnum.UTILISE);
        otpLivraisonRepository.save(otp);

        paiement.setStatutPaiement("LIBERE");
        paiementRepository.save(paiement);

        colis.setStatutColis("TERMINE");
        colisRepository.save(colis);

        // Notifier le transporteur
        notificationService.creer(userId,
            montant80 + " FCFA ont été transférés sur votre numéro " + numeroTransporteur + ". Livraison confirmée !",
            idColis, "PAIEMENT_LIBERE");

        // Notifier l'expéditeur
        notificationService.creer(colis.getIdUtilisateur(),
            "Livraison confirmée pour votre colis " + colis.getVilleDepart() + " → " + colis.getVilleArrive() + ".",
            idColis, "LIVRAISON_CONFIRMEE");

        // Email de confirmation au transporteur
        emailService.envoyerConfirmationLivraison(
            transporteur.getEmail(),
            transporteur.getPrenom() + " " + transporteur.getNom(),
            colis.getVilleDepart(), colis.getVilleArrive(),
            montant80.toPlainString()
        );

        return ResponseEntity.ok(Map.of(
            "message", "OTP validé. " + montant80 + " FCFA versés sur votre compte.",
            "montant", montant80,
            "statut", "LIBERE"
        ));
    }

    // ── GET /api/paiements/colis/{idColis} ────────────────────────────────────
    @GetMapping("/colis/{idColis}")
    public ResponseEntity<?> getPaiementColis(@PathVariable UUID idColis, Principal principal) {
        UUID userId = getUtilisateurId(principal);
        Colis colis = colisRepository.findById(idColis)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis non trouvé."));
        boolean ok = colis.getIdUtilisateur().equals(userId) ||
            (colis.getIdTransporteur() != null && colis.getIdTransporteur().equals(userId));
        if (!ok) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé.");
        return paiementRepository.findTopByIdColisOrderByDateDesc(idColis)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElse(ResponseEntity.ok(Map.of("statut", "AUCUN")));
    }

    // ── Génération OTP et envoi email destinataire ─────────────────────────────
    private synchronized void genererEtEnvoyerOtpDestinataire(Colis colis) {
        // Idempotence : si un OTP livraison actif existe déjà, pas de doublon
        if (otpLivraisonRepository.findByIdColisAndStatut(colis.getIdColis(), StatutOtpEnum.ACTIF).isPresent()) {
            System.out.println("[OTP-LIV] Un OTP livraison actif existe déjà pour le colis " + colis.getIdColis() + ", pas de régénération.");
            return;
        }

        String telephoneDest = colis.getTelephoneDestinataire();
        if (telephoneDest == null || telephoneDest.isBlank()) {
            System.out.println("[OTP-LIV] Pas de téléphone destinataire pour colis " + colis.getIdColis());
            return;
        }

        // Chercher le destinataire par son numéro (avec/sans préfixe 237)
        Optional<Utilisateur> destOpt = utilisateurRepository.findByNumeroPrincipal(telephoneDest);
        if (destOpt.isEmpty()) {
            String cleaned = telephoneDest.replaceAll("[^0-9]", "");
            if (cleaned.startsWith("237") && cleaned.length() > 9) {
                destOpt = utilisateurRepository.findByNumeroPrincipal(cleaned.substring(3));
            } else {
                destOpt = utilisateurRepository.findByNumeroPrincipal("237" + cleaned);
            }
        }

        if (destOpt.isEmpty()) {
            System.out.println("[OTP-LIV] Destinataire non trouvé pour numéro: " + telephoneDest);
            return;
        }

        Utilisateur destinataire = destOpt.get();

        // Générer un OTP à 6 chiffres
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));

        // Sauvegarder dans la table otp_livraison — complètement isolée de la table otp d'auth
        OtpLivraison otpLiv = new OtpLivraison();
        otpLiv.setIdColis(colis.getIdColis());
        otpLiv.setIdUtilisateur(destinataire.getIdUtilisateur());
        otpLiv.setCodeOtp(code);
        otpLiv.setDateExpiration(java.time.LocalDateTime.now().plusHours(72)); // 72h pour plus de confort
        otpLiv.setStatut(StatutOtpEnum.ACTIF);
        otpLivraisonRepository.save(otpLiv);

        // Envoyer par email
        emailService.envoyerOtpDestinataire(
            destinataire.getEmail(),
            destinataire.getPrenom() + " " + destinataire.getNom(),
            code,
            colis.getVilleDepart(),
            colis.getVilleArrive()
        );

        System.out.println("[OTP-LIV] Généré et envoyé à " + destinataire.getEmail() + " — expire dans 72h");
    }

    // Récupère l'OTP actif le plus récent pour un colis. Si des doublons existent
    // ── Récupère l'OTP livraison actif le plus récent depuis otp_livraison ──────
    private OtpLivraison getOtpLivraisonActif(UUID idColis) {
        java.util.List<OtpLivraison> actifs = otpLivraisonRepository
            .findByIdColisAndStatutOrderByDateExpirationDesc(idColis, StatutOtpEnum.ACTIF);

        if (actifs.isEmpty()) {
            System.out.println("[OTP-LIV] Aucun OTP livraison actif pour colis " + idColis);
            return null;
        }

        OtpLivraison plusRecent = actifs.get(0);
        if (actifs.size() > 1) {
            System.out.println("[OTP-LIV] " + actifs.size() + " doublons détectés pour colis " + idColis + ", nettoyage.");
            otpLivraisonRepository.expirerAutresOtpsActifs(idColis, plusRecent.getId());
        }
        System.out.println("[OTP-LIV] OTP actif trouvé — expire: " + plusRecent.getDateExpiration()
            + " — maintenant: " + java.time.LocalDateTime.now());
        return plusRecent;
    }

    private UUID getUtilisateurId(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié.");
        return utilisateurRepository.findByEmail(principal.getName().toLowerCase().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."))
            .getIdUtilisateur();
    }


    // ══════════════════════════════════════════════════════════════════════════
    // QR CODE : Le destinataire récupère le QR code de son colis
    // GET /api/paiements/colis/{idColis}/qr-code
    // Accessible : destinataire (trouvé par son numéro), expéditeur, transporteur
    // ══════════════════════════════════════════════════════════════════════════
    @GetMapping("/colis/{idColis}/qr-code")
    public ResponseEntity<?> getQrCode(@PathVariable UUID idColis, Principal principal) {

        UUID userId = getUtilisateurId(principal);
        Colis colis = colisRepository.findById(idColis)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis non trouvé."));

        // Vérifier que le paiement est bien effectué (séquestré ou libéré)
        paiementRepository.findTopByIdColisOrderByDateDesc(idColis)
            .filter(p -> "SEQUESTRE".equals(p.getStatutPaiement()) || "LIBERE".equals(p.getStatutPaiement()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Le QR code sera disponible une fois le paiement confirmé."));

        // Vérifier que l'utilisateur est autorisé (expéditeur, transporteur ou destinataire)
        boolean estExpediteur   = userId.equals(colis.getIdUtilisateur());
        boolean estTransporteur = userId.equals(colis.getIdTransporteur());
        boolean estDestinataire = false;

        if (colis.getTelephoneDestinataire() != null) {
            Utilisateur utilisateurCourant = utilisateurRepository.findById(userId).orElse(null);
            if (utilisateurCourant != null) {
                String numUser  = utilisateurCourant.getNumeroPrincipal().replaceAll("[^0-9]", "");
                String numDest  = colis.getTelephoneDestinataire().replaceAll("[^0-9]", "");
                estDestinataire = numUser.endsWith(numDest) || numDest.endsWith(numUser);
            }
        }

        if (!estExpediteur && !estTransporteur && !estDestinataire) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé.");
        }

        // Récupérer l'OTP actif (c'est lui qui encode le QR code) — version robuste : tolère et nettoie les doublons
        OtpLivraison otp = getOtpLivraisonActif(idColis);
        if (otp == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun code de livraison actif pour ce colis.");

        // Construire le contenu du QR : JSON avec idColis + code OTP
        String contenuQr = "{\"idColis\":\"" + idColis + "\",\"otp\":\"" + otp.getCodeOtp() + "\"}";

        return ResponseEntity.ok(Map.of(
            "idColis",      idColis.toString(),
            "contenuQr",    contenuQr,
            "codeOtp",      otp.getCodeOtp(),
            "villeDepart",  colis.getVilleDepart(),
            "villeArrive",  colis.getVilleArrive(),
            "expiration",   otp.getDateExpiration().toString(),
            "estDestinataire", estDestinataire
        ));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // VALIDER VIA QR CODE : Le voyageur scanne le QR → libère son argent
    // POST /api/paiements/colis/{idColis}/valider-qr
    // Body : { "contenuQr": "..." }  ou  { "otp": "..." }
    // ══════════════════════════════════════════════════════════════════════════
    @PostMapping("/colis/{idColis}/valider-qr")
    public ResponseEntity<?> validerParQr(
            @PathVariable UUID idColis,
            @RequestBody Map<String, String> body,
            Principal principal) {

        UUID userId = getUtilisateurId(principal);
        Colis colis = colisRepository.findById(idColis)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis non trouvé."));

        if (!userId.equals(colis.getIdTransporteur()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul le transporteur peut valider la livraison.");

        // Bloquer si le paiement est déjà libéré (double scan, attaque)
        Paiement paiementExistQr = paiementRepository.findTopByIdColisOrderByDateDesc(idColis)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paiement non trouvé."));
        if ("LIBERE".equals(paiementExistQr.getStatutPaiement()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Ce paiement a déjà été libéré. La livraison est confirmée.");

        // Extraire l'OTP depuis le contenu QR (JSON) ou directement
        String otpExtrait = null;
        String contenuQr = body.get("contenuQr");
        if (contenuQr != null && !contenuQr.isBlank()) {
            try {
                com.fasterxml.jackson.databind.JsonNode node =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(contenuQr);
                if (node.has("otp") && !node.get("otp").isNull()) {
                    otpExtrait = node.get("otp").asText();
                }
            } catch (Exception e) {
                System.err.println("[QR] Contenu QR non-JSON ou invalide: " + e.getMessage());
            }
        }
        if (otpExtrait == null || otpExtrait.isBlank()) otpExtrait = body.get("otp");

        if (otpExtrait == null || otpExtrait.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contenu QR invalide.");

        // Déléguer à la même logique (appel interne) — version robuste : tolère et nettoie les doublons
        OtpLivraison otp = getOtpLivraisonActif(idColis);
        if (otp == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "OTP introuvable ou déjà utilisé.");

        if (!otp.getCodeOtp().equals(otpExtrait.trim()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR code invalide ou ne correspond pas à ce colis.");

        if (otp.getDateExpiration().isBefore(java.time.LocalDateTime.now())) {
            otp.setStatut(StatutOtpEnum.EXPIRE);
            otpLivraisonRepository.save(otp);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR code expiré. Demandez un nouveau code au destinataire.");
        }

        // Marquer OTP utilisé : reporté après la confirmation du virement Campay
        // pour ne jamais "brûler" le code si le transfert échoue.

        // Paiement séquestré → libéré
        Paiement paiement = paiementRepository.findTopByIdColisOrderByDateDesc(idColis)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paiement non trouvé."));

        if (!"SEQUESTRE".equals(paiement.getStatutPaiement()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le paiement n'est pas en séquestre.");

        BigDecimal montant80 = paiement.getMontant()
            .multiply(new BigDecimal("0.80"))
            .setScale(0, RoundingMode.HALF_UP);

        Utilisateur transporteur = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transporteur non trouvé."));

        String numeroTransporteur = transporteur.getNumeroPrincipal();
        if (numeroTransporteur == null || numeroTransporteur.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Numéro principal du transporteur manquant.");

        try {
            JsonNode resp = campayService.initierTransfert(
                montant80.toPlainString(),
                numeroTransporteur,
                "Paiement livraison colis (QR) " + colis.getVilleDepart() + " → " + colis.getVilleArrive(),
                paiement.getIdPaiement().toString() + "_qr"
            );
            System.out.println("[QR-LIBERE] Campay réponse complète: " + resp);

            if (!campayWithdrawAccepte(resp)) {
                String detail = resp != null && resp.has("detail") ? resp.get("detail").asText()
                    : resp != null && resp.has("message") ? resp.get("message").asText()
                    : resp != null ? resp.toString() : "Réponse vide";
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Virement refusé: " + detail);
            }
            System.out.println("[QR-LIBERE] Virement accepté.");

        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Erreur Campay: " + e.getMessage() +
                ". Contactez le support avec la référence paiement : " + paiement.getIdPaiement());
        }

        // Virement confirmé : on peut maintenant marquer l'OTP utilisé et mettre à jour paiement + colis
        otp.setStatut(StatutOtpEnum.UTILISE);
        otpLivraisonRepository.save(otp);

        paiement.setStatutPaiement("LIBERE");
        paiementRepository.save(paiement);
        colis.setStatutColis("TERMINE");
        colisRepository.save(colis);

        // Notifier le transporteur
        notificationService.creer(userId,
            "✅ Livraison confirmée via QR code ! " + montant80 + " FCFA ont été transférés sur votre numéro.",
            idColis, "PAIEMENT_LIBERE");

        // Notifier l'expéditeur
        notificationService.creer(colis.getIdUtilisateur(),
            "✅ Livraison confirmée pour votre colis " + colis.getVilleDepart() + " → " + colis.getVilleArrive() +
                ". Le transporteur a bien remis le colis au destinataire.",
            idColis, "LIVRAISON_CONFIRMEE");

        // Notifier le destinataire
        try {
            UUID idDestinataire = chatService.trouverDestinataire(colis);
            if (idDestinataire != null) {
                notificationService.creer(
                    idDestinataire,
                    "🎉 Votre colis " + colis.getVilleDepart() + " → " + colis.getVilleArrive() +
                        " a été livré avec succès ! Merci d'avoir utilisé Colisender.",
                    idColis, "LIVRAISON_CONFIRMEE"
                );
            }
        } catch (Exception e) {
            System.err.println("[NOTIF-DEST] Impossible de notifier le destinataire (QR) : " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
            "statut",  "LIBERE",
            "montant", montant80,
            "message", "Livraison confirmée ! " + montant80 + " FCFA transférés sur votre numéro."
        ));
    }

    private boolean campayWithdrawAccepte(com.fasterxml.jackson.databind.JsonNode resp) {
        if (resp == null) return false;
        if (resp.has("reference") && !resp.get("reference").isNull() && !resp.get("reference").asText().isBlank()) return true;
        if (resp.has("withdraw_code") && !resp.get("withdraw_code").isNull() && !resp.get("withdraw_code").asText().isBlank()) return true;
        if (resp.has("status")) {
            String st = resp.get("status").asText();
            return "SUCCESSFUL".equalsIgnoreCase(st) || "PENDING".equalsIgnoreCase(st);
        }
        return false;
    }
}