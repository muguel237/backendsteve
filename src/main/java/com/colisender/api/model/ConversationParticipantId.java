package com.colisender.api.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ConversationParticipantId implements Serializable {

    private UUID idConversation;
    private UUID idUtilisateur;

    public ConversationParticipantId() {}

    public ConversationParticipantId(UUID idConversation, UUID idUtilisateur) {
        this.idConversation = idConversation;
        this.idUtilisateur = idUtilisateur;
    }

    public UUID getIdConversation() { return idConversation; }
    public void setIdConversation(UUID idConversation) { this.idConversation = idConversation; }

    public UUID getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(UUID idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConversationParticipantId)) return false;
        ConversationParticipantId that = (ConversationParticipantId) o;
        return Objects.equals(idConversation, that.idConversation) &&
               Objects.equals(idUtilisateur, that.idUtilisateur);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idConversation, idUtilisateur);
    }
}
