package com.colisender.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void envoyerOtpDestinataire(String email, String nomDestinataire,
                                        String otp, String villeDepart, String villeArrive) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Colisender - Code OTP pour la réception de votre colis");
            message.setText(
                "Bonjour " + nomDestinataire + ",\n\n" +
                "Votre colis en provenance de " + villeDepart + " à destination de " + villeArrive +
                " est en route !\n\n" +
                "Votre code OTP de confirmation de livraison est :\n\n" +
                "         " + otp + "\n\n" +
                "Communiquez ce code au livreur/voyageur au moment de la réception de votre colis.\n" +
                "Ce code est strictement confidentiel et ne doit être partagé qu'avec le livreur.\n\n" +
                "Cordialement,\nL'équipe Colisender"
            );
            mailSender.send(message);
            System.out.println("[EMAIL OTP] Envoyé à " + email);
        } catch (Exception e) {
            System.err.println("[EMAIL OTP] Échec envoi à " + email + ": " + e.getMessage());
        }
    }

    public void envoyerConfirmationLivraison(String email, String nomTransporteur,
                                              String villeDepart, String villeArrive,
                                              String montant) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Colisender - Paiement débloqué pour votre livraison");
            message.setText(
                "Bonjour,\n\n" +
                "La livraison du colis " + villeDepart + " → " + villeArrive + " a été confirmée.\n\n" +
                "Un montant de " + montant + " FCFA (80% du prix de transport) a été transféré\n" +
                "sur votre numéro principal.\n\n" +
                "Merci d'utiliser Colisender !\n\nL'équipe Colisender"
            );
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("[EMAIL LIVRAISON] Échec: " + e.getMessage());
        }
    }
}
