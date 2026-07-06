package com.colisender.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * OTP dédié aux livraisons de colis.
 * Complètement séparé de la table "otp" utilisée pour l'authentification,
 * ce qui évite tout risque d'expiration croisée entre les deux flux.
 */
@Entity
@Table(name = "otp_livraison")
public class OtpLivraison {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "id_colis", nullable = false, unique = true)
    private UUID idColis;

    @Column(name = "id_utilisateur", nullable = false)
    private UUID idUtilisateur; // destinataire du colis

    @Column(name = "code_otp", nullable = false)
    private String codeOtp;

    @Column(name = "date_expiration", nullable = false)
    private LocalDateTime dateExpiration;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutOtpEnum statut = StatutOtpEnum.ACTIF;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    // ── Getters / Setters ────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getIdColis() { return idColis; }
    public void setIdColis(UUID idColis) { this.idColis = idColis; }

    public UUID getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(UUID idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public String getCodeOtp() { return codeOtp; }
    public void setCodeOtp(String codeOtp) { this.codeOtp = codeOtp; }

    public LocalDateTime getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDateTime dateExpiration) { this.dateExpiration = dateExpiration; }

    public StatutOtpEnum getStatut() { return statut; }
    public void setStatut(StatutOtpEnum statut) { this.statut = statut; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
