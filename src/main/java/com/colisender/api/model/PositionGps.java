package com.colisender.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dernière position GPS connue d'un utilisateur pour un colis donné.
 * Une seule ligne par (idColis, idUtilisateur) — mise à jour à chaque envoi.
 */
@Entity
@Table(name = "suivi_gps")
public class PositionGps {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_position", updatable = false, nullable = false)
    private UUID idPosition;

    @Column(name = "id_colis", nullable = false)
    private UUID idColis;

    @Column(name = "id_utilisateur", nullable = false)
    private UUID idUtilisateur;

    @Column(name = "latitude", nullable = false)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false)
    private BigDecimal longitude;

    @Column(name = "date_position", nullable = false)
    private LocalDateTime datePosition = LocalDateTime.now();

    public PositionGps() {}

    public UUID getIdPosition() { return idPosition; }
    public void setIdPosition(UUID idPosition) { this.idPosition = idPosition; }

    public UUID getIdColis() { return idColis; }
    public void setIdColis(UUID idColis) { this.idColis = idColis; }

    public UUID getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(UUID idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public LocalDateTime getDatePosition() { return datePosition; }
    public void setDatePosition(LocalDateTime datePosition) { this.datePosition = datePosition; }
}
