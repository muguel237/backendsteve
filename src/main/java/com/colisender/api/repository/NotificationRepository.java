package com.colisender.api.repository;

import com.colisender.api.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByIdUtilisateurOrderByDateNotificationDesc(UUID idUtilisateur);

    long countByIdUtilisateurAndStatut(UUID idUtilisateur, com.colisender.api.model.StatutNotifEnum statut);
}
