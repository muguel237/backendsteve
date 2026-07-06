package com.colisender.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "paiement")
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_paiement", updatable = false, nullable = false)
    private UUID idPaiement;

    @Column(name = "id_colis", nullable = false)
    private UUID idColis;

    @Column(name = "montant", nullable = false)
    private BigDecimal montant;

    @Column(name = "methode_paiement", nullable = false)
    private String methodePaiement; // MTN_MOMO ou ORANGE_MONEY

    @Column(name = "statut_paiement", nullable = false)
    private String statutPaiement = "EN_ATTENTE";

    @Column(name = "date", nullable = false)
    private LocalDateTime date = LocalDateTime.now();

    // Référence Campay pour suivre la transaction
    @Column(name = "reference_campay")
    private String referenceCampay;

    // Numéro de téléphone utilisé pour le paiement
    @Column(name = "numero_telephone")
    private String numeroTelephone;

    // ID du voyageur/transporteur concerné par ce paiement (pour le versement final)
    @Column(name = "id_transporteur")
    private UUID idTransporteur;

    public Paiement() {}

    public UUID getIdPaiement() { return idPaiement; }
    public void setIdPaiement(UUID idPaiement) { this.idPaiement = idPaiement; }

    public UUID getIdColis() { return idColis; }
    public void setIdColis(UUID idColis) { this.idColis = idColis; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public String getMethodePaiement() { return methodePaiement; }
    public void setMethodePaiement(String methodePaiement) { this.methodePaiement = methodePaiement; }

    public String getStatutPaiement() { return statutPaiement; }
    public void setStatutPaiement(String statutPaiement) { this.statutPaiement = statutPaiement; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public String getReferenceCampay() { return referenceCampay; }
    public void setReferenceCampay(String referenceCampay) { this.referenceCampay = referenceCampay; }

    public String getNumeroTelephone() { return numeroTelephone; }
    public void setNumeroTelephone(String numeroTelephone) { this.numeroTelephone = numeroTelephone; }

    public UUID getIdTransporteur() { return idTransporteur; }
    public void setIdTransporteur(UUID idTransporteur) { this.idTransporteur = idTransporteur; }
}
