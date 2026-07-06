package com.colisender.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class UtilisateurAdminDto {
    private UUID idUtilisateur;
    private String nom;
    private String prenom;
    private String email;
    private String numeroPrincipal;
    private String photoProfile;
    private String statusCompte;
    private LocalDateTime dateCreation;
    private String statutVerification;
    private Double scoreCorrespondance;

    public UUID getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(UUID v) { this.idUtilisateur = v; }
    public String getNom() { return nom; }
    public void setNom(String v) { this.nom = v; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String v) { this.prenom = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getNumeroPrincipal() { return numeroPrincipal; }
    public void setNumeroPrincipal(String v) { this.numeroPrincipal = v; }
    public String getPhotoProfile() { return photoProfile; }
    public void setPhotoProfile(String v) { this.photoProfile = v; }
    public String getStatusCompte() { return statusCompte; }
    public void setStatusCompte(String v) { this.statusCompte = v; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime v) { this.dateCreation = v; }
    public String getStatutVerification() { return statutVerification; }
    public void setStatutVerification(String v) { this.statutVerification = v; }
    public Double getScoreCorrespondance() { return scoreCorrespondance; }
    public void setScoreCorrespondance(Double v) { this.scoreCorrespondance = v; }
}
