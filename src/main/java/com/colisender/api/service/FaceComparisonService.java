package com.colisender.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class FaceComparisonService {

    @Value("${faceplusplus.api-key}")
    private String apiKey;

    @Value("${faceplusplus.api-secret}")
    private String apiSecret;

    @Value("${faceplusplus.seuil-confiance:76.0}")
    private double seuilConfiance;

    private static final String FACEPP_COMPARE_URL = "https://api-us.faceplusplus.com/facepp/v3/compare";
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Compare le visage de la CNI recto avec le selfie.
     *
     * @param photoCNI   Photo recto de la CNI (doit contenir un visage lisible)
     * @param photoSelfie Selfie pris par l'utilisateur
     * @return résultat de comparaison avec score et décision
     * @throws FaceComparisonException si les visages sont absents ou si la confiance est insuffisante
     */
    public ResultatComparaison comparerVisages(MultipartFile photoCNI, MultipartFile photoSelfie) throws Exception {

        // Clés non configurées → mode permissif (dev/test)
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("VOTRE")) {
            System.out.println("[FACE++] Clés non configurées — vérification biométrique ignorée (mode dev).");
            return new ResultatComparaison(true, 100.0, "Mode développement : vérification ignorée.");
        }

        String boundary = "----ColisenderBoundary" + System.currentTimeMillis();
        byte[] corpsRequete = construireCorpsMultipart(boundary, photoCNI, photoSelfie);

        URL url = new URL(FACEPP_COMPARE_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setRequestProperty("Content-Length", String.valueOf(corpsRequete.length));

        try (OutputStream os = conn.getOutputStream()) {
            os.write(corpsRequete);
        }

        int statusCode = conn.getResponseCode();
        InputStream is = (statusCode >= 400) ? conn.getErrorStream() : conn.getInputStream();
        String reponseJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        System.out.println("[FACE++] Statut HTTP : " + statusCode);
        System.out.println("[FACE++] Réponse : " + reponseJson);

        JsonNode root = objectMapper.readTree(reponseJson);

        // Gérer les erreurs Face++
        if (root.has("error_message")) {
            String erreur = root.get("error_message").asText();
            return traiterErreurFacepp(erreur);
        }

        if (!root.has("confidence")) {
            throw new FaceComparisonException(
                "Réponse inattendue de l'API de reconnaissance faciale. Veuillez réessayer.");
        }

        double confidence = root.get("confidence").asDouble();
        String thresholds = root.has("thresholds")
            ? root.get("thresholds").toString() : "{}";

        System.out.println("[FACE++] Score de confiance : " + confidence + " / seuil : " + seuilConfiance);
        System.out.println("[FACE++] Seuils Face++ : " + thresholds);

        boolean estLaMemePersonne = confidence >= seuilConfiance;
        String message = estLaMemePersonne
            ? String.format("Vérification biométrique réussie (confiance : %.1f%%)", confidence)
            : String.format("Le selfie ne correspond pas au visage sur la CNI (confiance : %.1f%% / seuil : %.1f%%)",
                confidence, seuilConfiance);

        return new ResultatComparaison(estLaMemePersonne, confidence, message);
    }

   
  
    private byte[] construireCorpsMultipart(String boundary, MultipartFile photoCNI, MultipartFile photoSelfie)
            throws IOException {

        String base64CNI     = Base64.getEncoder().encodeToString(photoCNI.getBytes());
        String base64Selfie  = Base64.getEncoder().encodeToString(photoSelfie.getBytes());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(bos, StandardCharsets.UTF_8), true);

        // api_key
        ajouterChampTexte(writer, boundary, "api_key", apiKey);
        // api_secret
        ajouterChampTexte(writer, boundary, "api_secret", apiSecret);
        // image_base64_1 = photo CNI recto
        ajouterChampTexte(writer, boundary, "image_base64_1", base64CNI);
        // image_base64_2 = selfie
        ajouterChampTexte(writer, boundary, "image_base64_2", base64Selfie);

        // Fermeture
        writer.append("--").append(boundary).append("--\r\n");
        writer.flush();

        return bos.toByteArray();
    }

    private void ajouterChampTexte(PrintWriter writer, String boundary, String nom, String valeur) {
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"").append(nom).append("\"").append("\r\n");
        writer.append("Content-Type: text/plain; charset=UTF-8").append("\r\n");
        writer.append("\r\n");
        writer.append(valeur).append("\r\n");
        writer.flush();
    }

  
   private ResultatComparaison traiterErreurFacepp(String erreur) throws FaceComparisonException {
    System.err.println("[FACE++] Erreur API : " + erreur);

    switch (erreur) {

        case "INVALID_API_KEY":
        case "AUTHENTICATION_ERROR":
            throw new FaceComparisonException(
                "Erreur de configuration du service de vérification. Contactez le support.");

        case "NO_FACE_FOUND":
        case "IMAGE_ERROR_UNSUPPORTED_FORMAT":
            throw new FaceComparisonException(
                "Aucun visage détecté dans l'une des photos. Assurez-vous que :\n" +
                "• Votre CNI est bien éclairée et lisible\n" +
                "• Votre selfie montre clairement votre visage de face");

        case "IMAGE_FILE_TOO_LARGE":
            throw new FaceComparisonException(
                "L'une des photos est trop lourde. Veuillez réduire la taille des fichiers (max 2 Mo).");

        case "CONCURRENCY_LIMIT_EXCEEDED":
            throw new FaceComparisonException(
                "Le service de vérification est momentanément surchargé. Réessayez dans quelques secondes.");

        default:
            throw new FaceComparisonException(
                "Erreur lors de la vérification biométrique : " + erreur +
                ". Vérifiez la qualité de vos photos et réessayez.");
    }
}

 

    public static class ResultatComparaison {
        private final boolean estLaMemePersonne;
        private final double scoreConfiance;
        private final String message;

        public ResultatComparaison(boolean estLaMemePersonne, double scoreConfiance, String message) {
            this.estLaMemePersonne = estLaMemePersonne;
            this.scoreConfiance = scoreConfiance;
            this.message = message;
        }

        public boolean estLaMemePersonne() { return estLaMemePersonne; }
        public double getScoreConfiance()   { return scoreConfiance; }
        public String getMessage()          { return message; }
    }

    public static class FaceComparisonException extends Exception {
        public FaceComparisonException(String message) { super(message); }
    }
}
