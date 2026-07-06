package com.colisender.api.controller;

import com.colisender.api.dto.NumerosRequest;
import com.colisender.api.dto.ProfilDto;
import com.colisender.api.service.ProfilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/profil")
public class ProfilController {

    @Autowired
    private ProfilService profilService;

    /**
     * GET /api/profil/{userId}
     * Retourne les donnees publiques du profil (jamais le mot de passe).
     * Correspond a chargerProfil() dans Profil.jsx.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ProfilDto> getProfil(@PathVariable UUID userId) {
        return ResponseEntity.ok(profilService.getProfil(userId));
    }

    /**
     * PATCH /api/profil/{userId}/numeros
     * Modifie numeroPrincipal et numeroSecondaire.
     * Correspond a sauvegarderNumeros() dans Profil.jsx.
     * Retourne le profil mis a jour (le frontend lit data.numeroPrincipal / data.numeroSecondaire).
     */
    @PatchMapping("/{userId}/numeros")
    public ResponseEntity<ProfilDto> updateNumeros(
            @PathVariable UUID userId,
            @RequestBody NumerosRequest req) {
        return ResponseEntity.ok(profilService.updateNumeros(userId, req));
    }

    /**
     * POST /api/profil/{userId}/photo
     * Upload d'une nouvelle photo de profil (multipart/form-data, champ "photo").
     * Correspond a changerPhoto() dans Profil.jsx.
     * Retourne { ..., photoProfil: "profil_xxx.jpg" }
     * Le frontend construit l'URL : http://localhost:8080/uploads/{photoProfil}
     */
    @PostMapping("/{userId}/photo")
    public ResponseEntity<ProfilDto> updatePhoto(
            @PathVariable UUID userId,
            @RequestParam("photo") MultipartFile photo) throws IOException {
        return ResponseEntity.ok(profilService.updatePhoto(userId, photo));
    }
}
