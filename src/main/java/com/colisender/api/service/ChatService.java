package com.colisender.api.service;

import com.colisender.api.model.*;
import com.colisender.api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChatService {

    public static final String TYPE_EXP_VOY = "EXPEDITEUR_VOYAGEUR";
    public static final String TYPE_VOY_DEST = "VOYAGEUR_DESTINATAIRE";

    @Autowired private ConversationRepository conversationRepository;
    @Autowired private ConversationParticipantRepository participantRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private ColisRepository colisRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;

    /**
     * Crée (si besoin) les DEUX conversations séparées pour un colis :
     *  1. EXPEDITEUR_VOYAGEUR
     *  2. VOYAGEUR_DESTINATAIRE (uniquement si le destinataire a un compte)
     * Appelé automatiquement quand le paiement passe en SEQUESTRE.
     */
    public void creerConversationsPourColis(UUID idColis) {
        Colis colis = colisRepository.findById(idColis)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis non trouvé."));

        if (colis.getIdTransporteur() == null) return;

        // 1. Conversation Expéditeur <-> Voyageur
        getOuCreerConversation(idColis, TYPE_EXP_VOY,
            colis.getIdUtilisateur(), colis.getIdTransporteur());

        // 2. Conversation Voyageur <-> Destinataire (si compte trouvé)
        UUID idDestinataire = trouverDestinataire(colis);
        if (idDestinataire != null) {
            getOuCreerConversation(idColis, TYPE_VOY_DEST,
                colis.getIdTransporteur(), idDestinataire);
        }
    }

    private Conversation getOuCreerConversation(UUID idColis, String type, UUID... participants) {
        Optional<Conversation> existante = conversationRepository.findByIdColisAndTypeConversation(idColis, type);
        Conversation conv;
        if (existante.isPresent()) {
            conv = existante.get();
        } else {
            conv = new Conversation();
            conv.setIdColis(idColis);
            conv.setTypeConversation(type);
            conv = conversationRepository.save(conv);
        }
        for (UUID p : participants) {
            ajouterParticipant(conv.getIdConversation(), p);
        }
        return conv;
    }

    private void ajouterParticipant(UUID idConversation, UUID idUtilisateur) {
        if (idUtilisateur == null) return;
        if (!participantRepository.existsByIdConversationAndIdUtilisateur(idConversation, idUtilisateur)) {
            participantRepository.save(new ConversationParticipant(idConversation, idUtilisateur));
        }
    }

    /**
     * Recherche le compte Colisender du destinataire via son numéro de téléphone.
     */
    public UUID trouverDestinataire(Colis colis) {
        String telephoneDest = colis.getTelephoneDestinataire();
        if (telephoneDest == null || telephoneDest.isBlank()) return null;

        String cleaned = telephoneDest.replaceAll("[^0-9]", "");

        Optional<Utilisateur> dest = utilisateurRepository.findByNumeroPrincipal(telephoneDest);
        if (dest.isEmpty() && cleaned.startsWith("237") && cleaned.length() > 9) {
            dest = utilisateurRepository.findByNumeroPrincipal(cleaned.substring(3));
        }
        if (dest.isEmpty() && !cleaned.startsWith("237")) {
            dest = utilisateurRepository.findByNumeroPrincipal("237" + cleaned);
        }
        if (dest.isEmpty()) {
            dest = utilisateurRepository.findByNumeroPrincipal(cleaned);
        }
        return dest.map(Utilisateur::getIdUtilisateur).orElse(null);
    }

    /**
     * Liste de toutes les conversations de l'utilisateur, triées par dernier message.
     */
    public List<Conversation> mesConversations(UUID idUtilisateur) {
        return participantRepository.findConversationsByUtilisateur(idUtilisateur);
    }

    /**
     * Conversation d'un type donné pour un colis, pour l'utilisateur courant.
     * 404 si elle n'existe pas encore ou si l'utilisateur n'y participe pas.
     */
    public Conversation getConversation(UUID idColis, String type, UUID idUtilisateur) {
        return conversationRepository.findByIdColisAndTypeAndParticipant(idColis, type, idUtilisateur)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Cette discussion n'est pas encore disponible."));
    }

    public Message envoyerMessage(UUID idConversation, UUID idExpediteur, String contenu) {
        verifierParticipant(idConversation, idExpediteur);
        Message m = new Message();
        m.setIdConversation(idConversation);
        m.setIdExpediteur(idExpediteur);
        m.setContenu(contenu);
        return messageRepository.save(m);
    }

    public List<Message> getMessages(UUID idConversation, UUID idUtilisateur) {
        verifierParticipant(idConversation, idUtilisateur);
        List<Message> messages = messageRepository.findByIdConversationOrderByDateEnvoiAsc(idConversation);
        for (Message m : messages) {
            if (!m.getIdExpediteur().equals(idUtilisateur) && "ENVOYE".equals(m.getStatutLecture())) {
                m.setStatutLecture("LU");
                messageRepository.save(m);
            }
        }
        return messages;
    }


    /**
     * Supprime une conversation et tous ses messages.
     * Uniquement autorisé si le colis est livré (TERMINE ou LIVRE).
     */
    public void supprimerConversation(UUID idConversation, UUID idUtilisateur) {
        // Vérifier que l'utilisateur participe
        verifierParticipant(idConversation, idUtilisateur);

        // Récupérer la conversation pour vérifier le statut du colis
        Conversation conv = conversationRepository.findById(idConversation)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation introuvable."));

        Colis colis = colisRepository.findById(conv.getIdColis())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis introuvable."));

        String statut = colis.getStatutColis();
        if (!"TERMINE".equals(statut) && !"LIVRE".equals(statut) && !"ANNULE".equals(statut)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "La conversation ne peut être supprimée qu'après la livraison du colis.");
        }

        // Supprimer messages → participants → conversation
        messageRepository.deleteByIdConversation(idConversation);
        participantRepository.deleteByIdConversation(idConversation);
        conversationRepository.deleteById(idConversation);
    }

    public List<Utilisateur> getAutresParticipants(UUID idConversation, UUID idUtilisateurCourant) {
        List<ConversationParticipant> participants = participantRepository.findByIdConversation(idConversation);
        List<Utilisateur> autres = new ArrayList<>();
        for (ConversationParticipant p : participants) {
            if (!p.getIdUtilisateur().equals(idUtilisateurCourant)) {
                utilisateurRepository.findById(p.getIdUtilisateur()).ifPresent(autres::add);
            }
        }
        return autres;
    }

    private void verifierParticipant(UUID idConversation, UUID idUtilisateur) {
        if (!participantRepository.existsByIdConversationAndIdUtilisateur(idConversation, idUtilisateur)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous ne participez pas à cette conversation.");
        }
    }
}
