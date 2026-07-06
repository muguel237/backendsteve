package com.colisender.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une conversation est liée à un colis ET à un "type" :
 *  - EXPEDITEUR_VOYAGEUR : discussion entre l'expéditeur et le transporteur
 *  - VOYAGEUR_DESTINATAIRE : discussion entre le transporteur et le destinataire
 *
 * Cela permet d'avoir DEUX chats séparés pour un même colis.
 */
@Entity
@Table(name = "conversation")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_conversation", updatable = false, nullable = false)
    private UUID idConversation;

    @Column(name = "id_colis", nullable = false)
    private UUID idColis;

    /** EXPEDITEUR_VOYAGEUR | VOYAGEUR_DESTINATAIRE */
    @Column(name = "type_conversation", nullable = false)
    private String typeConversation;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    public Conversation() {}

    public UUID getIdConversation() { return idConversation; }
    public void setIdConversation(UUID idConversation) { this.idConversation = idConversation; }

    public UUID getIdColis() { return idColis; }
    public void setIdColis(UUID idColis) { this.idColis = idColis; }

    public String getTypeConversation() { return typeConversation; }
    public void setTypeConversation(String typeConversation) { this.typeConversation = typeConversation; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
