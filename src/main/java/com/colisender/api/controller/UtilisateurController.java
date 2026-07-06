package com.colisender.api.controller;

import com.colisender.api.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints publics liés aux utilisateurs.
 */
@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurController {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    /**
     * GET /api/utilisateurs/verifier-numero?numero=6XXXXXXXX
     *
     * Vérifie si un numéro de téléphone correspond à un compte Colisender
     * (numéro principal OU secondaire), en testant toutes les variantes de
     * format (avec/sans préfixe 237).
     *
     * Endpoint PUBLIC : utilisé par le formulaire de publication de colis
     * pour valider le numéro du destinataire avant soumission.
     *
     * Répond :
     *   { "existe": true,  "prenom": "Alice", "nom": "D." }
     *   { "existe": false }
     */
    @GetMapping("/verifier-numero")
    public ResponseEntity<Map<String, Object>> verifierNumero(@RequestParam String numero) {
        if (numero == null || numero.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("existe", false, "message", "Numéro vide."));
        }

        // Normalise : retire tout ce qui n'est pas un chiffre
        String cleaned = numero.replaceAll("[^0-9]", "");

        // Génère les variantes : avec/sans préfixe 237
        java.util.Set<String> variantes = new java.util.LinkedHashSet<>();
        variantes.add(cleaned);
        if (cleaned.startsWith("237") && cleaned.length() > 9) {
            variantes.add(cleaned.substring(3));          // 237671... → 671...
        } else if (!cleaned.startsWith("237")) {
            variantes.add("237" + cleaned);               // 671... → 237671...
        }

        for (String v : variantes) {
            var parPrincipal  = utilisateurRepository.findByNumeroPrincipal(v);
            if (parPrincipal.isPresent()) {
                var u = parPrincipal.get();
                return ResponseEntity.ok(Map.of(
                    "existe", true,
                    "prenom", u.getPrenom() != null ? u.getPrenom() : "",
                    "nom",    u.getNom()    != null ? String.valueOf(u.getNom().charAt(0)) + "." : ""
                ));
            }
            var parSecondaire = utilisateurRepository.findByNumeroSecondaire(v);
            if (parSecondaire.isPresent()) {
                var u = parSecondaire.get();
                return ResponseEntity.ok(Map.of(
                    "existe", true,
                    "prenom", u.getPrenom() != null ? u.getPrenom() : "",
                    "nom",    u.getNom()    != null ? String.valueOf(u.getNom().charAt(0)) + "." : ""
                ));
            }
        }

        return ResponseEntity.ok(Map.of("existe", false));
    }
}
