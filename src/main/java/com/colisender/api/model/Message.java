package com.colisender.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_message", updatable = false, nullable = false)
    private UUID idMessage;

    @Column(name = "id_conversation", nullable = false)
    private UUID idConversation;

    @Column(name = "id_expediteur", nullable = false)
    private UUID idExpediteur;

    @Column(name = "contenu", nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Column(name = "date_envoi", nullable = false)
    private LocalDateTime dateEnvoi = LocalDateTime.now();

    @Column(name = "statut_lecture", nullable = false)
    private String statutLecture = "ENVOYE"; // ENVOYE | LU

    public Message() {}

    public UUID getIdMessage() { return idMessage; }
    public void setIdMessage(UUID idMessage) { this.idMessage = idMessage; }

    public UUID getIdConversation() { return idConversation; }
    public void setIdConversation(UUID idConversation) { this.idConversation = idConversation; }

    public UUID getIdExpediteur() { return idExpediteur; }
    public void setIdExpediteur(UUID idExpediteur) { this.idExpediteur = idExpediteur; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }

    public String getStatutLecture() { return statutLecture; }
    public void setStatutLecture(String statutLecture) { this.statutLecture = statutLecture; }
}
