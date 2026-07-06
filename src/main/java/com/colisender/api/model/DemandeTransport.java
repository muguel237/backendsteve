package com.colisender.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "demande_transport",
    uniqueConstraints = @UniqueConstraint(columnNames = {"colis_id", "voyageur_id"}))
public class DemandeTransport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "colis_id", nullable = false)
    private UUID colisId;

    @Column(name = "voyageur_id", nullable = false)
    private UUID voyageurId;

    @Column(name = "date_demande", nullable = false)
    private LocalDateTime dateDemande = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutDemandeEnum statut = StatutDemandeEnum.EN_ATTENTE;

    // Getters / Setters
    public UUID getId() { return id; }
    public UUID getColisId() { return colisId; }
    public void setColisId(UUID colisId) { this.colisId = colisId; }
    public UUID getVoyageurId() { return voyageurId; }
    public void setVoyageurId(UUID voyageurId) { this.voyageurId = voyageurId; }
    public LocalDateTime getDateDemande() { return dateDemande; }
    public StatutDemandeEnum getStatut() { return statut; }
    public void setStatut(StatutDemandeEnum statut) { this.statut = statut; }
}
