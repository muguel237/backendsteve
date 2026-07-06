package com.colisender.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ColisAnnonceDto {
    private UUID idColis;
    private String description;
    private BigDecimal poids;
    private BigDecimal dimension;
    private String villeDepart;
    private String villeArrive;
    private String adresseRecuperation;
    private String adresseLivraison;
    private String statutColis;
    private BigDecimal prixTransport;
    private LocalDate dateCreation;
    private LocalDate dateLivraison;
    private String telephoneDestinataire;
    private List<String> photos;

    // Infos sur l'expéditeur
    private UUID idExpediteur;
    private String nomExpediteur;
    private String prenomExpediteur;
    private String photoExpediteur;

    // A-t-on déjà postulé ?
    private boolean dejaPostule;

    public UUID getIdColis() { return idColis; }
    public void setIdColis(UUID idColis) { this.idColis = idColis; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPoids() { return poids; }
    public void setPoids(BigDecimal poids) { this.poids = poids; }

    public BigDecimal getDimension() { return dimension; }
    public void setDimension(BigDecimal dimension) { this.dimension = dimension; }

    public String getVilleDepart() { return villeDepart; }
    public void setVilleDepart(String villeDepart) { this.villeDepart = villeDepart; }

    public String getVilleArrive() { return villeArrive; }
    public void setVilleArrive(String villeArrive) { this.villeArrive = villeArrive; }

    public String getAdresseRecuperation() { return adresseRecuperation; }
    public void setAdresseRecuperation(String adresseRecuperation) { this.adresseRecuperation = adresseRecuperation; }

    public String getAdresseLivraison() { return adresseLivraison; }
    public void setAdresseLivraison(String adresseLivraison) { this.adresseLivraison = adresseLivraison; }

    public String getStatutColis() { return statutColis; }
    public void setStatutColis(String statutColis) { this.statutColis = statutColis; }

    public BigDecimal getPrixTransport() { return prixTransport; }
    public void setPrixTransport(BigDecimal prixTransport) { this.prixTransport = prixTransport; }

    public LocalDate getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDate dateCreation) { this.dateCreation = dateCreation; }

    public LocalDate getDateLivraison() { return dateLivraison; }
    public void setDateLivraison(LocalDate dateLivraison) { this.dateLivraison = dateLivraison; }

    public String getTelephoneDestinataire() { return telephoneDestinataire; }
    public void setTelephoneDestinataire(String telephoneDestinataire) { this.telephoneDestinataire = telephoneDestinataire; }

    public List<String> getPhotos() { return photos; }
    public void setPhotos(List<String> photos) { this.photos = photos; }

    public UUID getIdExpediteur() { return idExpediteur; }
    public void setIdExpediteur(UUID idExpediteur) { this.idExpediteur = idExpediteur; }

    public String getNomExpediteur() { return nomExpediteur; }
    public void setNomExpediteur(String nomExpediteur) { this.nomExpediteur = nomExpediteur; }

    public String getPrenomExpediteur() { return prenomExpediteur; }
    public void setPrenomExpediteur(String prenomExpediteur) { this.prenomExpediteur = prenomExpediteur; }

    public String getPhotoExpediteur() { return photoExpediteur; }
    public void setPhotoExpediteur(String photoExpediteur) { this.photoExpediteur = photoExpediteur; }

    public boolean isDejaPostule() { return dejaPostule; }
    public void setDejaPostule(boolean dejaPostule) { this.dejaPostule = dejaPostule; }
}
