package com.colisender.api.service;

import com.colisender.api.model.Notification;
import com.colisender.api.model.StatutNotifEnum;
import com.colisender.api.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;
 @Autowired
    private com.colisender.api.repository.UtilisateurRepository utilisateurRepository;
    public Notification creer(UUID idUtilisateur, String contenu, UUID idColis, String typeNotif) {
        Notification n = new Notification(idUtilisateur, contenu, idColis, typeNotif);
        return notificationRepository.save(n);
    }

    public List<Notification> listerPourUtilisateur(UUID idUtilisateur) {
        return notificationRepository.findByIdUtilisateurOrderByDateNotificationDesc(idUtilisateur);
    }

    public long compterNonLues(UUID idUtilisateur) {
        return notificationRepository.countByIdUtilisateurAndStatut(idUtilisateur, StatutNotifEnum.ENVOYE);
    }

    public Notification marquerLu(UUID idNotification, UUID idUtilisateur) {
        Notification n = notificationRepository.findById(idNotification)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification non trouvée"));
        if (!n.getIdUtilisateur().equals(idUtilisateur)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé.");
        }
        n.setStatut(StatutNotifEnum.LU);
        return notificationRepository.save(n);
    }

    public void marquerToutesLues(UUID idUtilisateur) {
        List<Notification> notifs = notificationRepository.findByIdUtilisateurOrderByDateNotificationDesc(idUtilisateur);
        for (Notification n : notifs) {
            if (n.getStatut() == StatutNotifEnum.ENVOYE) {
                n.setStatut(StatutNotifEnum.LU);
                notificationRepository.save(n);
            }
        }
    }
    public int broadcastToAll(String contenu, String typeNotif) {
        java.util.List<com.colisender.api.model.Utilisateur> tousLesUsers =
            utilisateurRepository.findAll();

        int count = 0;
        for (com.colisender.api.model.Utilisateur u : tousLesUsers) {
            Notification n = new Notification(
                u.getIdUtilisateur(),
                contenu,
                null,           // pas de colis lié
                typeNotif
            );
            notificationRepository.save(n);
            count++;
        }
        return count;
    }

    // ── Supprimer une notification (vérifie que ça appartient à l'utilisateur) ──
    public void supprimer(UUID idNotification, UUID idUtilisateur) {
        Notification n = notificationRepository.findById(idNotification)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Notification introuvable."));
        if (!n.getIdUtilisateur().equals(idUtilisateur))
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "Accès refusé.");
        notificationRepository.deleteById(idNotification);
    }

    // ── Supprimer toutes les notifications de l'utilisateur ───────────────
    public void supprimerToutes(UUID idUtilisateur) {
        List<Notification> notifs = notificationRepository
            .findByIdUtilisateurOrderByDateNotificationDesc(idUtilisateur);
        notificationRepository.deleteAll(notifs);
    }

}
