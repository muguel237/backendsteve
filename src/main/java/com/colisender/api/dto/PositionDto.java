package com.colisender.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PositionDto {
    private UUID idUtilisateur;
    private String nom;
    private String prenom;
    private String role; // VOYAGEUR | DESTINATAIRE
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime datePosition;

    public UUID getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(UUID idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public LocalDateTime getDatePosition() { return datePosition; }
    public void setDatePosition(LocalDateTime datePosition) { this.datePosition = datePosition; }
}
