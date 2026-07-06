package com.colisender.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class LitigeDto {
    private UUID idLitige;
    private UUID idColis;
    private String descriptionColis;
    private String villeDepart;
    private String villeArrive;
    private String nomExpediteur;
    private String nomTransporteur;
    private String description;
    private String statutLitige;
    private LocalDateTime dateCreation;
    private UUID idAdministrateur;

    public UUID getIdLitige() { return idLitige; }
    public void setIdLitige(UUID v) { this.idLitige = v; }
    public UUID getIdColis() { return idColis; }
    public void setIdColis(UUID v) { this.idColis = v; }
    public String getDescriptionColis() { return descriptionColis; }
    public void setDescriptionColis(String v) { this.descriptionColis = v; }
    public String getVilleDepart() { return villeDepart; }
    public void setVilleDepart(String v) { this.villeDepart = v; }
    public String getVilleArrive() { return villeArrive; }
    public void setVilleArrive(String v) { this.villeArrive = v; }
    public String getNomExpediteur() { return nomExpediteur; }
    public void setNomExpediteur(String v) { this.nomExpediteur = v; }
    public String getNomTransporteur() { return nomTransporteur; }
    public void setNomTransporteur(String v) { this.nomTransporteur = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getStatutLitige() { return statutLitige; }
    public void setStatutLitige(String v) { this.statutLitige = v; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime v) { this.dateCreation = v; }
    public UUID getIdAdministrateur() { return idAdministrateur; }
    public void setIdAdministrateur(UUID v) { this.idAdministrateur = v; }
}
