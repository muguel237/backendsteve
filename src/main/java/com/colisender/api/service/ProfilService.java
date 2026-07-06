package com.colisender.api.service;

import com.colisender.api.dto.NumerosRequest;
import com.colisender.api.dto.ProfilDto;
import com.colisender.api.model.Utilisateur;
import com.colisender.api.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class ProfilService {

    private static final String UPLOAD_DIR =
        System.getProperty("user.home") + "/colisender_uploads/profils/";

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    // ── GET profil ─────────────────────────────────────────────────────────
    public ProfilDto getProfil(UUID userId) {
        Utilisateur u = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Utilisateur non trouve"));
        return toDto(u);
    }

    // ── PATCH numeros ──────────────────────────────────────────────────────
    public ProfilDto updateNumeros(UUID userId, NumerosRequest req) {
        Utilisateur u = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Utilisateur non trouve"));

        // Validation numeroPrincipal : obligatoire, 9 chiffres commencant par 6
        String principal = req.getNumeroPrincipal();
        if (principal == null || !principal.matches("^6[0-9]{8}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Numero principal invalide. Format attendu : 6XXXXXXXX (9 chiffres).");
        }

        // Validation numeroSecondaire : optionnel, mais si fourni, meme format
        String secondaire = req.getNumeroSecondaire();
        if (secondaire != null && !secondaire.isBlank() && !secondaire.matches("^6[0-9]{8}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Numero secondaire invalide. Format attendu : 6XXXXXXXX (9 chiffres).");
        }

        u.setNumeroPrincipal(principal);
        u.setNumeroSecondaire(secondaire != null && secondaire.isBlank() ? null : secondaire);
        utilisateurRepository.save(u);

        return toDto(u);
    }

    // ── POST photo ─────────────────────────────────────────────────────────
    public ProfilDto updatePhoto(UUID userId, MultipartFile photo) throws IOException {
        Utilisateur u = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Utilisateur non trouve"));

        // Validation type MIME
        String contentType = photo.getContentType();
        if (contentType == null ||
            !(contentType.equals("image/jpeg") ||
              contentType.equals("image/png")  ||
              contentType.equals("image/webp"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Format non supporte. Utilisez JPG, PNG ou WEBP.");
        }

        // Validation taille (5 Mo max)
        if (photo.getSize() > 5L * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Image trop lourde. Maximum 5 Mo.");
        }

        // Supprimer l'ancienne photo si elle existe
        if (u.getPhotoProfil() != null && !u.getPhotoProfil().isBlank()) {
            Path ancienne = Paths.get(UPLOAD_DIR, u.getPhotoProfil());
            try { Files.deleteIfExists(ancienne); } catch (IOException ignored) {}
        }

        // Extension du fichier
        String ext = ".jpg";
        if ("image/png".equals(contentType))  ext = ".png";
        if ("image/webp".equals(contentType)) ext = ".webp";

        // Nom unique pour eviter les collisions
        String fileName = "profil_" + userId + "_" + UUID.randomUUID() + ext;

        // Creer le repertoire si besoin et sauvegarder
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
        Files.copy(photo.getInputStream(),
                   uploadPath.resolve(fileName),
                   StandardCopyOption.REPLACE_EXISTING);

        // Mettre a jour la BD avec le nom du fichier seulement
        u.setPhotoProfil(fileName);
        utilisateurRepository.save(u);

        return toDto(u);
    }

    // ── Conversion Utilisateur -> ProfilDto ────────────────────────────────
    private ProfilDto toDto(Utilisateur u) {
        ProfilDto dto = new ProfilDto();
        dto.setIdUtilisateur(u.getIdUtilisateur());
        dto.setNom(u.getNom());
        dto.setPrenom(u.getPrenom());
        dto.setEmail(u.getEmail());
        dto.setNumeroPrincipal(u.getNumeroPrincipal());
        dto.setNumeroSecondaire(u.getNumeroSecondaire());
        dto.setPhotoProfil(u.getPhotoProfil());           // juste le nom du fichier
        dto.setStatusCompte(u.getStatusCompte() != null  // "ACTIF" | "EN_ATTENTE" | ...
            ? u.getStatusCompte().name() : "EN_ATTENTE");
        return dto;
    }
}
