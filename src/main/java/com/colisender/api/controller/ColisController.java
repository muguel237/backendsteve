package com.colisender.api.controller;
 
import com.colisender.api.model.*;
import com.colisender.api.dto.ColisAnnonceDto;
import com.colisender.api.repository.*;
import com.colisender.api.service.ColisService;
import com.colisender.api.service.NotificationService;
import com.colisender.api.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
 
@RestController
@RequestMapping("/api/colis")
public class ColisController {
 
    @Autowired private ColisRepository colisRepository;
    @Autowired private PhotoColisRepository photoColisRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private ColisTransporteurRepository colisTransporteurRepository; // table colis_transporteur
    @Autowired private ColisService colisService;
    @Autowired private NotificationService notificationService;
    @Autowired private ChatService chatService;
    @Autowired private com.colisender.api.repository.PaiementRepository paiementRepository;
 
    private final String UPLOAD_DIR = System.getProperty("user.home") + "/colisender_uploads/colis/";
 
    // ── GET mes-colis ──────────────────────────────────────────────────────────
    @GetMapping("/mes-colis")
    public List<Colis> getMesColis(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
        Utilisateur user = getUtilisateur(principal);
        List<Colis> mesColis = colisRepository.findByIdUtilisateur(user.getIdUtilisateur());
        for (Colis c : mesColis) {
            paiementRepository.findTopByIdColisOrderByDateDesc(c.getIdColis()).ifPresent(p -> {
                if ("SEQUESTRE".equals(p.getStatutPaiement()) || "LIBERE".equals(p.getStatutPaiement())) {
                    c.setPaiementEffectue(true);
                }
            });
        }
        return mesColis;
    }
 
    // ── GET mes-colis-recus — colis dont je suis le destinataire ───────────────
    // Le destinataire est identifié par son numéro de téléphone (pas de lien direct
    // en base), via la même logique que ChatService.trouverDestinataire.
    @GetMapping("/mes-colis-recus")
    public List<Colis> getMesColisRecus(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
        Utilisateur user = getUtilisateur(principal);

        String numero = user.getNumeroPrincipal();
        if (numero == null || numero.isBlank()) return new ArrayList<>();

        String cleaned = numero.replaceAll("[^0-9]", "");
        java.util.Set<String> variantes = new java.util.HashSet<>();
        variantes.add(numero);
        variantes.add(cleaned);
        if (cleaned.startsWith("237") && cleaned.length() > 9) {
            variantes.add(cleaned.substring(3));
        } else {
            variantes.add("237" + cleaned);
        }

        List<Colis> candidats = colisRepository.findByTelephoneDestinataireIn(variantes);

        // Exclut les colis que l'utilisateur a lui-même créés (il est expéditeur, pas destinataire)
        // et revérifie via la logique centrale pour éviter tout faux positif de suffixe.
        List<Colis> colisRecus = candidats.stream()
            .filter(c -> !c.getIdUtilisateur().equals(user.getIdUtilisateur()))
            .collect(Collectors.toList());

        for (Colis c : colisRecus) {
            paiementRepository.findTopByIdColisOrderByDateDesc(c.getIdColis()).ifPresent(p -> {
                if ("SEQUESTRE".equals(p.getStatutPaiement()) || "LIBERE".equals(p.getStatutPaiement())) {
                    c.setPaiementEffectue(true);
                }
            });
        }
        return colisRecus;
    }

    // ── GET annonces — colis disponibles pour les voyageurs ────────────────────
    // Accessible aux visiteurs non connectés (principal peut être null)
    @GetMapping("/annonces")
    public ResponseEntity<List<ColisAnnonceDto>> getAnnonces(
            @RequestParam(value = "villeDepart",  required = false) String villeDepart,
            @RequestParam(value = "villeArrivee", required = false) String villeArrivee,
            @RequestParam(value = "poidsMax",     required = false) java.math.BigDecimal poidsMax,
            Principal principal) {

        // Visiteur non connecté → UUID aléatoire qui ne matchera aucun utilisateur
        final UUID userId = (principal != null)
            ? getUtilisateurId(principal)
            : UUID.randomUUID();

        List<Colis> colisList;
        if (villeDepart != null && !villeDepart.isBlank()) {
            colisList = colisRepository.findByStatutColisAndIdUtilisateurNotAndVilleDepartContainingIgnoreCase(
                "EN_ATTENTE", userId, villeDepart.trim());
        } else {
            colisList = colisRepository.findByStatutColisAndIdUtilisateurNot("EN_ATTENTE", userId);
        }

        // Filtre ville d'arrivée (en mémoire, léger)
        if (villeArrivee != null && !villeArrivee.isBlank()) {
            final String va = villeArrivee.trim().toLowerCase();
            colisList = colisList.stream()
                .filter(c -> c.getVilleArrive() != null && c.getVilleArrive().toLowerCase().contains(va))
                .collect(java.util.stream.Collectors.toList());
        }

        // Filtre poids max
        if (poidsMax != null) {
            colisList = colisList.stream()
                .filter(c -> c.getPoids() != null && c.getPoids().compareTo(poidsMax) <= 0)
                .collect(java.util.stream.Collectors.toList());
        }

        List<ColisAnnonceDto> result = colisList.stream().map(c -> {
            ColisAnnonceDto dto = new ColisAnnonceDto();
            dto.setIdColis(c.getIdColis());
            dto.setDescription(c.getDescription());
            dto.setPoids(c.getPoids());
            dto.setDimension(c.getDimension());
            dto.setVilleDepart(c.getVilleDepart());
            dto.setVilleArrive(c.getVilleArrive());
            dto.setAdresseRecuperation(c.getAdresseRecuperation());
            dto.setAdresseLivraison(c.getAdresseLivraison());
            dto.setStatutColis(c.getStatutColis());
            dto.setPrixTransport(c.getPrixTransport());
            dto.setDateCreation(c.getDateCreation());
            dto.setDateLivraison(c.getDateLivraison());
            dto.setTelephoneDestinataire(c.getTelephoneDestinataire());

            List<String> photos = photoColisRepository.findByColis_IdColis(c.getIdColis())
                .stream().map(PhotoColis::getUrlPhoto).collect(Collectors.toList());
            dto.setPhotos(photos);

            Utilisateur expediteur = utilisateurRepository.findById(c.getIdUtilisateur()).orElse(null);
            if (expediteur != null) {
                dto.setIdExpediteur(expediteur.getIdUtilisateur());
                dto.setNomExpediteur(expediteur.getNom());
                dto.setPrenomExpediteur(expediteur.getPrenom());
                dto.setPhotoExpediteur(expediteur.getPhotoProfil());
            }

            dto.setDejaPostule(colisTransporteurRepository.existsByIdColisAndIdUtilisateur(c.getIdColis(), userId));

            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }


    // ── GET mes-transports — colis assignés à ce voyageur ────────────────────
    @GetMapping("/mes-transports")
    public ResponseEntity<List<Map<String, Object>>> getMesTransports(Principal principal) {
        UUID userId = getUtilisateurId(principal);
        List<Colis> colisList = colisRepository.findByIdTransporteur(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Colis c : colisList) {
            Map<String, Object> dto = new HashMap<>();
            dto.put("idColis", c.getIdColis());
            dto.put("villeDepart", c.getVilleDepart());
            dto.put("villeArrive", c.getVilleArrive());
            dto.put("adresseRecuperation", c.getAdresseRecuperation());
            dto.put("adresseLivraison", c.getAdresseLivraison());
            dto.put("prixTransport", c.getPrixTransport());
            dto.put("statutColis", c.getStatutColis());
            dto.put("telephoneDestinataire", c.getTelephoneDestinataire());
            boolean paiementOk = paiementRepository.findTopByIdColisOrderByDateDesc(c.getIdColis())
                .map(p -> "SEQUESTRE".equals(p.getStatutPaiement()) || "LIBERE".equals(p.getStatutPaiement()))
                .orElse(false);
            dto.put("paiementEffectue", paiementOk);
            utilisateurRepository.findById(c.getIdUtilisateur()).ifPresent(exp -> {
                dto.put("nomExpediteur", exp.getNom());
                dto.put("prenomExpediteur", exp.getPrenom());
            });
            result.add(dto);
        }
        return ResponseEntity.ok(result);
    }


    // ── GET /{idColis}/statut — statut simple d'un colis (tous rôles) ────────────
    @GetMapping("/{idColis}/statut")
    public ResponseEntity<Map<String, String>> getStatutColis(
            @PathVariable UUID idColis, Principal principal) {
        // Vérifier que l'utilisateur est authentifié
        getUtilisateur(principal);
        Colis colis = colisRepository.findById(idColis)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis introuvable."));
        return ResponseEntity.ok(Map.of("statutColis", colis.getStatutColis()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteColis(@PathVariable UUID id, Principal principal) {
        UUID userId = getUtilisateurId(principal);
        colisService.deleteColis(id, userId);
        return ResponseEntity.noContent().build();
    }
 
    // ── PUT /{id} ──────────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<Colis> modifierColis(
            @PathVariable UUID id,
            @RequestBody Colis colisDetails,
            Principal principal) {
        UUID userId = getUtilisateurId(principal);
        Colis colisMisAJour = colisService.updateColis(id, colisDetails, userId);
        return ResponseEntity.ok(colisMisAJour);
    }
 
    // ── GET /{id}/postulants — liste des voyageurs intéressés ─────────────────
    @GetMapping("/{id}/postulants")
    public ResponseEntity<List<Utilisateur>> getPostulants(
            @PathVariable UUID id, Principal principal) {
        UUID userId = getUtilisateurId(principal);
 
        Colis colis = colisRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis non trouvé"));
 
        // Seul le propriétaire du colis peut voir les postulants
        if (!colis.getIdUtilisateur().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé.");
        }
 
        // Utilise la table colis_transporteur de la BD
        List<Utilisateur> voyageurs = colisTransporteurRepository.findVoyageursByColisId(id);
        return ResponseEntity.ok(voyageurs);
    }
 
    // ── POST /{id}/postuler — un voyageur postule pour transporter ─────────────
    @PostMapping("/{id}/postuler")
    public ResponseEntity<Void> postuler(@PathVariable UUID id, Principal principal) {
        UUID voyageurId = getUtilisateurId(principal);
 
        Colis colis = colisRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis non trouvé"));
 
        // Ne pas postuler sur son propre colis
        if (colis.getIdUtilisateur().equals(voyageurId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous ne pouvez pas postuler sur votre propre colis.");
        }
 
        if (!"EN_ATTENTE".equals(colis.getStatutColis())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce colis n'est plus disponible.");
        }
 
        // Vérifier si déjà postulé (clé composite)
        if (colisTransporteurRepository.existsByIdColisAndIdUtilisateur(id, voyageurId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous avez déjà postulé pour ce colis.");
        }
 
        // Insérer dans colis_transporteur
        ColisTransporteur ct = new ColisTransporteur(id, voyageurId);
        colisTransporteurRepository.save(ct);

        // Notification pour l'expéditeur : un voyageur a postulé
        Utilisateur voyageur = getUtilisateur(principal);
        String nomVoyageur = voyageur.getPrenom() + " " + voyageur.getNom();
        notificationService.creer(
            colis.getIdUtilisateur(),
            "Le voyageur " + nomVoyageur + " a postulé pour transporter votre colis (" +
                colis.getVilleDepart() + " → " + colis.getVilleArrive() + ").",
            colis.getIdColis(),
            "POSTULATION"
        );

        // Confirmation pour le voyageur
        notificationService.creer(
            voyageurId,
            "Votre candidature pour le colis " + colis.getVilleDepart() + " → " + colis.getVilleArrive() + " a été envoyée à l'expéditeur.",
            colis.getIdColis(),
            "POSTULATION_ENVOYEE"
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // ── POST /{id}/choisir-transporteur/{voyageurId} ───────────────────────────
    @PostMapping("/{id}/choisir-transporteur/{voyageurId}")
    public ResponseEntity<Colis> choisirTransporteur(
            @PathVariable UUID id,
            @PathVariable UUID voyageurId,
            Principal principal) {

        UUID userId = getUtilisateurId(principal);

        Colis colis = colisRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis non trouvé"));

        if (!colis.getIdUtilisateur().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé.");
        }

        if (!"EN_ATTENTE".equals(colis.getStatutColis())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce colis n'est plus disponible.");
        }

        if (!colisTransporteurRepository.existsByIdColisAndIdUtilisateur(id, voyageurId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce voyageur n'a pas postulé pour ce colis.");
        }

        if (!utilisateurRepository.existsById(voyageurId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voyageur non trouvé");
        }

        // Marquer le colis comme attribué (en attente de paiement)
        colis.setStatutColis("EN_COURS");
        colis.setIdTransporteur(voyageurId);
        Colis updated = colisRepository.save(colis);

        // Notification pour le voyageur choisi
        notificationService.creer(
            voyageurId,
            "Vous avez été choisi pour transporter le colis " + colis.getVilleDepart() + " → " + colis.getVilleArrive() +
                ". En attente du paiement de l'expéditeur.",
            colis.getIdColis(),
            "TRANSPORTEUR_CHOISI"
        );

        // Notification pour les autres postulants non retenus
        List<Utilisateur> tousPostulants = colisTransporteurRepository.findVoyageursByColisId(id);
        for (Utilisateur p : tousPostulants) {
            if (!p.getIdUtilisateur().equals(voyageurId)) {
                notificationService.creer(
                    p.getIdUtilisateur(),
                    "Votre candidature pour le colis " + colis.getVilleDepart() + " → " + colis.getVilleArrive() +
                        " n'a pas été retenue.",
                    colis.getIdColis(),
                    "POSTULATION_REFUSEE"
                );
            }
        }

        return ResponseEntity.ok(updated);
    }

    @PostMapping
    public Colis createColis(
            @ModelAttribute Colis colis,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
            Principal principal
    ) throws IOException {
 
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
 
        Utilisateur user = getUtilisateur(principal);
        colis.setUtilisateur(user);
        colis.setIdUtilisateur(user.getIdUtilisateur());
        colis.setStatutColis("EN_ATTENTE");
 
        Colis savedColis = colisRepository.save(colis);
 
        if (photos != null && !photos.isEmpty()) {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
 
            for (MultipartFile photo : photos) {
                String fileName = "colis_" + savedColis.getIdColis() + "_" + UUID.randomUUID() + ".jpg";
                Files.copy(photo.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
 
                PhotoColis pc = new PhotoColis();
                pc.setUrlPhoto(fileName);
                pc.setColis(savedColis);
                photoColisRepository.save(pc);
            }
        }
        return savedColis;
    }
 
    // ── Helpers ────────────────────────────────────────────────────────────────
    private Utilisateur getUtilisateur(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return utilisateurRepository.findByEmail(principal.getName().toLowerCase().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }
 
    private UUID getUtilisateurId(Principal principal) {
        return getUtilisateur(principal).getIdUtilisateur();
    }
}