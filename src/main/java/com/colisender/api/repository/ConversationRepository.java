package com.colisender.api.repository;

import com.colisender.api.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findByIdColis(UUID idColis);

    Optional<Conversation> findByIdColisAndTypeConversation(UUID idColis, String typeConversation);

    @Query("SELECT c FROM Conversation c WHERE c.idColis = :idColis AND c.typeConversation = :type AND c.idConversation IN " +
           "(SELECT cp.idConversation FROM ConversationParticipant cp WHERE cp.idUtilisateur = :idUtilisateur)")
    Optional<Conversation> findByIdColisAndTypeAndParticipant(@Param("idColis") UUID idColis,
                                                               @Param("type") String typeConversation,
                                                               @Param("idUtilisateur") UUID idUtilisateur);
}
