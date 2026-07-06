package com.colisender.api.model;
 
import jakarta.persistence.*;
import java.util.UUID;
 
/**
 * Mappe la table colis_transporteur de la BD.
 * Représente la candidature d'un voyageur pour transporter un colis.
 */
@Entity
@Table(name = "colis_transporteur")
@IdClass(ColisTransporteurId.class)
public class ColisTransporteur {
 
    @Id
    @Column(name = "id_colis", nullable = false)
    private UUID idColis;
 
    @Id
    @Column(name = "id_utilisateur", nullable = false)
    private UUID idUtilisateur;
 
    public ColisTransporteur() {}
 
    public ColisTransporteur(UUID idColis, UUID idUtilisateur) {
        this.idColis = idColis;
        this.idUtilisateur = idUtilisateur;
    }
 
    public UUID getIdColis() { return idColis; }
    public void setIdColis(UUID idColis) { this.idColis = idColis; }
 
    public UUID getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(UUID idUtilisateur) { this.idUtilisateur = idUtilisateur; }
}
