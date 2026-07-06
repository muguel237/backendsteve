package com.colisender.api.repository;

import com.colisender.api.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByIdConversationOrderByDateEnvoiAsc(UUID idConversation);
    long countByIdConversationAndStatutLectureAndIdExpediteurNot(UUID idConversation, String statutLecture, UUID idExpediteur);
    void deleteByIdConversation(UUID idConversation);
}
