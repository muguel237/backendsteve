package com.colisender.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "litige")
public class Litige {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_litige", updatable = false, nullable = false)
    private UUID idLitige;

    @Column(name = "id_colis", nullable = false)
    private UUID idColis;

    @Column(name = "id_administrateur")
    private UUID idAdministrateur;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "statut_litige", nullable = false)
    private String statutLitige = "OUVERT";

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    public Litige() {}

    public UUID getIdLitige() { return idLitige; }
    public void setIdLitige(UUID idLitige) { this.idLitige = idLitige; }

    public UUID getIdColis() { return idColis; }
    public void setIdColis(UUID idColis) { this.idColis = idColis; }

    public UUID getIdAdministrateur() { return idAdministrateur; }
    public void setIdAdministrateur(UUID idAdministrateur) { this.idAdministrateur = idAdministrateur; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatutLitige() { return statutLitige; }
    public void setStatutLitige(String statutLitige) { this.statutLitige = statutLitige; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
