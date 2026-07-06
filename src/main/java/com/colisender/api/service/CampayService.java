package com.colisender.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class CampayService {

    @Value("${campay.base-url}")
    private String baseUrl;

    @Value("${campay.permanent-token}")
    private String permanentToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpHeaders buildAuthHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Authorization", "Token " + permanentToken);
        return h;
    }

    private JsonNode safeParse(String json) {
        if (json == null || json.isBlank()) {
            // Corps vide → on retourne un objet JSON d'erreur exploitable
            // au lieu de planter silencieusement
            try {
                return objectMapper.readTree("{\"status\":\"EMPTY_RESPONSE\",\"detail\":\"Campay a retourné un corps vide\"}");
            } catch (JsonProcessingException ex) {
                throw new RuntimeException("Réponse Campay vide", ex);
            }
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erreur de format JSON Campay: " + e.getMessage(), e);
        }
    }

    /**
     * Nettoie l'external_reference pour Campay :
     * - Supprime les tirets UUID  (- interdit)
     * - Supprime les underscores  (_ interdit)
     * - Tronque à 40 caractères max
     * Ex: "550e8400-e29b-41d4-a716_qr" → "550e8400e29b41d4a716qr"
     */
    private String sanitizeRef(String ref) {
        if (ref == null) return "REF";
        String clean = ref.replaceAll("[-_]", "");   // supprime - et _
        return clean.length() > 40 ? clean.substring(0, 40) : clean;
    }

    // ── Collecte (paiement entrant) ───────────────────────────────────────────
    public JsonNode initierCollecte(String amount, String phone, String description, String externalReference) {
        String url = baseUrl + "/collect/";
        Map<String, Object> body = new HashMap<>();
        body.put("amount",             amount);
        body.put("from",               normalizePhone(phone));
        body.put("description",        description);
        body.put("external_reference", sanitizeRef(externalReference));

        System.out.println("[CAMPAY-COLLECT] Envoi → montant=" + amount
            + " from=" + normalizePhone(phone)
            + " ref=" + sanitizeRef(externalReference));

        return executerAvecRetry(() -> {
            HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, buildAuthHeaders());
            try {
                ResponseEntity<String> resp = restTemplate.postForEntity(url, req, String.class);
                System.out.println("[CAMPAY-COLLECT] Réponse HTTP " + resp.getStatusCode() + " : " + resp.getBody());
                return safeParse(resp.getBody());
            } catch (HttpClientErrorException e) {
                System.err.println("[CAMPAY-COLLECT] Erreur HTTP " + e.getStatusCode() + " : " + e.getResponseBodyAsString());
                return safeParse(e.getResponseBodyAsString());
            }
        }, "collect");
    }

    // ── Vérification statut ───────────────────────────────────────────────────
    public JsonNode verifierStatut(String reference) {
        String url = baseUrl + "/transaction/" + reference + "/";
        return executerAvecRetry(() -> {
            HttpEntity<Void> req = new HttpEntity<>(buildAuthHeaders());
            try {
                ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, req, String.class);
                return safeParse(resp.getBody());
            } catch (HttpClientErrorException e) {
                System.err.println("[CAMPAY-STATUT] Erreur HTTP " + e.getStatusCode() + " : " + e.getResponseBodyAsString());
                return safeParse(e.getResponseBodyAsString());
            }
        }, "transaction");
    }

    // ── Virement sortant (/withdraw/) ─────────────────────────────────────────
    public JsonNode initierTransfert(String amount, String phone, String description, String externalReference) {
        String url = baseUrl + "/withdraw/";
        Map<String, Object> body = new HashMap<>();
        body.put("amount",             amount);
        body.put("to",                 normalizePhone(phone));
        body.put("description",        description);
        body.put("external_reference", sanitizeRef(externalReference));

        System.out.println("[CAMPAY-WITHDRAW] Envoi → montant=" + amount
            + " to=" + normalizePhone(phone)
            + " ref=" + sanitizeRef(externalReference));

        return executerAvecRetry(() -> {
            HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, buildAuthHeaders());
            try {
                ResponseEntity<String> resp = restTemplate.postForEntity(url, req, String.class);
                System.out.println("[CAMPAY-WITHDRAW] Réponse HTTP " + resp.getStatusCode() + " : " + resp.getBody());
                return safeParse(resp.getBody());
            } catch (HttpClientErrorException e) {
                System.err.println("[CAMPAY-WITHDRAW] Erreur HTTP " + e.getStatusCode() + " : " + e.getResponseBodyAsString());
                return safeParse(e.getResponseBodyAsString());
            }
        }, "withdraw");
    }

    public String normalizePhone(String phone) {
        if (phone == null) return null;
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("237")) return cleaned;
        if (cleaned.length() == 9) return "237" + cleaned;
        return cleaned;
    }

    private JsonNode executerAvecRetry(Supplier<JsonNode> appel, String nomOp) {
        for (int i = 1; i <= 3; i++) {
            try {
                return appel.get();
            } catch (Exception e) {
                if (i == 3) throw new RuntimeException(
                    "Erreur Campay [" + nomOp + "] après 3 tentatives: " + e.getMessage(), e);
                try { Thread.sleep(1000L * i); } catch (InterruptedException ignored) {}
            }
        }
        return null;
    }
}
