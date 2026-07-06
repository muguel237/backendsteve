package com.colisender.api.repository;

import com.colisender.api.model.Conversation;
import com.colisender.api.model.ConversationParticipant;
import com.colisender.api.model.ConversationParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ConversationParticipantRepository
        extends JpaRepository<ConversationParticipant, ConversationParticipantId> {

    boolean existsByIdConversationAndIdUtilisateur(UUID idConversation, UUID idUtilisateur);

    List<ConversationParticipant> findByIdConversation(UUID idConversation);

    @Query("SELECT c FROM Conversation c JOIN ConversationParticipant p ON c.idConversation = p.idConversation WHERE p.idUtilisateur = :idUtilisateur ORDER BY c.dateCreation DESC")
    List<Conversation> findConversationsByUtilisateur(@Param("idUtilisateur") UUID idUtilisateur);

    // ── Suppression de tous les participants d'une conversation ──────────────────
    void deleteByIdConversation(UUID idConversation);
}
