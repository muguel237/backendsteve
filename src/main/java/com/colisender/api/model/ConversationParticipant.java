package com.colisender.api.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "conversation_participants")
@IdClass(ConversationParticipantId.class)
public class ConversationParticipant {

    @Id
    @Column(name = "id_conversation")
    private UUID idConversation;

    @Id
    @Column(name = "id_utilisateur")
    private UUID idUtilisateur;

    public ConversationParticipant() {}

    public ConversationParticipant(UUID idConversation, UUID idUtilisateur) {
        this.idConversation = idConversation;
        this.idUtilisateur = idUtilisateur;
    }

    public UUID getIdConversation() { return idConversation; }
    public void setIdConversation(UUID idConversation) { this.idConversation = idConversation; }

    public UUID getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(UUID idUtilisateur) { this.idUtilisateur = idUtilisateur; }
}
