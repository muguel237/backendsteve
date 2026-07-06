package com.colisender.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_notification", updatable = false, nullable = false)
    private UUID idNotification;

    @Column(name = "id_utilisateur", nullable = false)
    private UUID idUtilisateur;

    @Column(name = "contenu", nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutNotifEnum statut = StatutNotifEnum.ENVOYE;

    @Column(name = "date_notification", nullable = false)
    private LocalDateTime dateNotification = LocalDateTime.now();

    // Champs additionnels non persistés directement (lien optionnel vers un colis)
    @Column(name = "id_colis")
    private UUID idColis;

    @Column(name = "type_notif")
    private String typeNotif;

    public Notification() {}

    public Notification(UUID idUtilisateur, String contenu, UUID idColis, String typeNotif) {
        this.idUtilisateur = idUtilisateur;
        this.contenu = contenu;
        this.idColis = idColis;
        this.typeNotif = typeNotif;
    }

    public UUID getIdNotification() { return idNotification; }
    public void setIdNotification(UUID idNotification) { this.idNotification = idNotification; }

    public UUID getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(UUID idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public StatutNotifEnum getStatut() { return statut; }
    public void setStatut(StatutNotifEnum statut) { this.statut = statut; }

    public LocalDateTime getDateNotification() { return dateNotification; }
    public void setDateNotification(LocalDateTime dateNotification) { this.dateNotification = dateNotification; }

    public UUID getIdColis() { return idColis; }
    public void setIdColis(UUID idColis) { this.idColis = idColis; }

    public String getTypeNotif() { return typeNotif; }
    public void setTypeNotif(String typeNotif) { this.typeNotif = typeNotif; }
}
