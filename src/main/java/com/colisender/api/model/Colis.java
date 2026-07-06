package com.colisender.api.model;
 
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
 
@Entity
@Table(name = "colis")
public class Colis {
 
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_colis")
    private UUID idColis;
 
    @Column(name = "id_utilisateur", nullable = false)
    private UUID idUtilisateur;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", insertable = false, updatable = false)
    private Utilisateur utilisateur;
 
    @Column(name = "description")
    private String description;
 
    @Column(name = "poids", nullable = false)
    private BigDecimal poids;
 
    @Column(name = "dimension")
    private BigDecimal dimension;
 
    @Column(name = "ville_depart", nullable = false)
    private String villeDepart;
 
    @Column(name = "ville_arrive", nullable = false)
    private String villeArrive;
 
    @Column(name = "adresse_recuperation", nullable = false)
    private String adresseRecuperation;
 
    @Column(name = "adresse_livraison", nullable = false)
    private String adresseLivraison;
 
    @Column(name = "statut", nullable = false)
    private String statutColis = "EN_ATTENTE";
 
    @Column(name = "prix_transport", nullable = false)
    private BigDecimal prixTransport;
 
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDate dateCreation = LocalDate.now();
 
    @Column(name = "date_livraison")
    private LocalDate dateLivraison;
 
    @Column(name = "telephone_destinataire")
    private String telephoneDestinataire;

    @Column(name = "id_transporteur")
    private UUID idTransporteur;

    public Colis() {}

    public UUID getIdColis() { return idColis; }
    public void setIdColis(UUID idColis) { this.idColis = idColis; }

    public UUID getIdTransporteur() { return idTransporteur; }
    public void setIdTransporteur(UUID idTransporteur) { this.idTransporteur = idTransporteur; }

    @Transient
    private boolean paiementEffectue = false;

    public boolean isPaiementEffectue() { return paiementEffectue; }
    public void setPaiementEffectue(boolean paiementEffectue) { this.paiementEffectue = paiementEffectue; }
 
    public UUID getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(UUID idUtilisateur) { this.idUtilisateur = idUtilisateur; }
 
    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
        if (utilisateur != null) {
            this.idUtilisateur = utilisateur.getIdUtilisateur();
        }
    }
 
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
}
