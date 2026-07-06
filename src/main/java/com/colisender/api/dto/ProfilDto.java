package com.colisender.api.dto;

import java.util.UUID;

/**
 * DTO retourné par GET /api/profil/{userId}.
 * Ne contient jamais le mot de passe.
 * Les champs correspondent exactement aux clés attendues par Profil.jsx.
 */
public class ProfilDto {

    private UUID   idUtilisateur;
    private String nom;
    private String prenom;
    private String email;
    private String numeroPrincipal;
    private String numeroSecondaire;
    private String photoProfil;   // URL complète ou chemin serveur
    private String statusCompte;  // "ACTIF" | "EN_ATTENTE" | "SUSPENDU"

    public ProfilDto() {}

    public UUID   getIdUtilisateur()    { return idUtilisateur; }
    public String getNom()              { return nom; }
    public String getPrenom()           { return prenom; }
    public String getEmail()            { return email; }
    public String getNumeroPrincipal()  { return numeroPrincipal; }
    public String getNumeroSecondaire() { return numeroSecondaire; }
    public String getPhotoProfil()      { return photoProfil; }
    public String getStatusCompte()     { return statusCompte; }

    public void setIdUtilisateur(UUID v)    { this.idUtilisateur = v; }
    public void setNom(String v)            { this.nom = v; }
    public void setPrenom(String v)         { this.prenom = v; }
    public void setEmail(String v)          { this.email = v; }
    public void setNumeroPrincipal(String v) { this.numeroPrincipal = v; }
    public void setNumeroSecondaire(String v){ this.numeroSecondaire = v; }
    public void setPhotoProfil(String v)    { this.photoProfil = v; }
    public void setStatusCompte(String v)   { this.statusCompte = v; }
}
