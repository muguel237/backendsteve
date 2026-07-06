package com.colisender.api.controller;

import com.colisender.api.dto.ConversationDto;
import com.colisender.api.dto.MessageRequest;
import com.colisender.api.model.*;
import com.colisender.api.repository.ColisRepository;
import com.colisender.api.repository.MessageRepository;
import com.colisender.api.repository.UtilisateurRepository;
import com.colisender.api.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired private ChatService chatService;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private ColisRepository colisRepository;
    @Autowired private MessageRepository messageRepository;

    /**
     * GET /api/chat/colis/{idColis}/expediteur-voyageur
     * Conversation entre l'expéditeur et le voyageur (pour ce colis).
     */
    @GetMapping("/colis/{idColis}/expediteur-voyageur")
    public ResponseEntity<ConversationDto> conversationExpVoy(@PathVariable UUID idColis, Principal principal) {
        UUID userId = getUtilisateurId(principal);
        Conversation conv = chatService.getConversation(idColis, ChatService.TYPE_EXP_VOY, userId);
        return ResponseEntity.ok(toDto(conv, userId));
    }

    /**
     * GET /api/chat/colis/{idColis}/voyageur-destinataire
     * Conversation entre le voyageur et le destinataire (pour ce colis).
     */
    @GetMapping("/colis/{idColis}/voyageur-destinataire")
    public ResponseEntity<ConversationDto> conversationVoyDest(@PathVariable UUID idColis, Principal principal) {
        UUID userId = getUtilisateurId(principal);
        Conversation conv = chatService.getConversation(idColis, ChatService.TYPE_VOY_DEST, userId);
        return ResponseEntity.ok(toDto(conv, userId));
    }

    // ── GET /api/chat/mes-conversations — liste de toutes mes discussions ───────
    @GetMapping("/mes-conversations")
    public ResponseEntity<List<ConversationDto>> mesConversations(Principal principal) {
        UUID userId = getUtilisateurId(principal);
        List<Conversation> convs = chatService.mesConversations(userId);
        List<ConversationDto> result = new ArrayList<>();
        for (Conversation c : convs) {
            result.add(toDto(c, userId));
        }
        return ResponseEntity.ok(result);
    }

    // ── GET /api/chat/{idConversation}/messages ─────────────────────────────────
    @GetMapping("/{idConversation}/messages")
    public ResponseEntity<List<Message>> messages(@PathVariable UUID idConversation, Principal principal) {
        UUID userId = getUtilisateurId(principal);
        return ResponseEntity.ok(chatService.getMessages(idConversation, userId));
    }

    // ── POST /api/chat/{idConversation}/messages ────────────────────────────────
    @PostMapping("/{idConversation}/messages")
    public ResponseEntity<Message> envoyer(
            @PathVariable UUID idConversation,
            @RequestBody MessageRequest req,
            Principal principal) {
        UUID userId = getUtilisateurId(principal);
        if (req.getContenu() == null || req.getContenu().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le message ne peut pas être vide.");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(chatService.envoyerMessage(idConversation, userId, req.getContenu().trim()));
    }

    // ── DELETE /api/chat/{idConversation} — supprimer une conversation après livraison ──
    @DeleteMapping("/{idConversation}")
    public ResponseEntity<Void> supprimerConversation(
            @PathVariable UUID idConversation, Principal principal) {
        UUID userId = getUtilisateurId(principal);
        chatService.supprimerConversation(idConversation, userId);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private ConversationDto toDto(Conversation conv, UUID userId) {
        ConversationDto dto = new ConversationDto();
        dto.setIdConversation(conv.getIdConversation());
        dto.setIdColis(conv.getIdColis());
        dto.setTypeConversation(conv.getTypeConversation());
        dto.setDateCreation(conv.getDateCreation());

        Colis colis = colisRepository.findById(conv.getIdColis()).orElse(null);
        if (colis != null) {
            dto.setVilleDepart(colis.getVilleDepart());
            dto.setVilleArrive(colis.getVilleArrive());
            dto.setStatutColis(colis.getStatutColis());

            List<ConversationDto.ParticipantDto> participants = new ArrayList<>();
            for (Utilisateur u : chatService.getAutresParticipants(conv.getIdConversation(), userId)) {
                ConversationDto.ParticipantDto p = new ConversationDto.ParticipantDto();
                p.setIdUtilisateur(u.getIdUtilisateur());
                p.setNom(u.getNom());
                p.setPrenom(u.getPrenom());
                p.setPhotoProfil(u.getPhotoProfil());

                if (u.getIdUtilisateur().equals(colis.getIdUtilisateur())) {
                    p.setRole("EXPEDITEUR");
                } else if (colis.getIdTransporteur() != null && u.getIdUtilisateur().equals(colis.getIdTransporteur())) {
                    p.setRole("VOYAGEUR");
                } else {
                    p.setRole("DESTINATAIRE");
                }
                participants.add(p);
            }
            dto.setParticipants(participants);
        }

        List<Message> msgs = messageRepository.findByIdConversationOrderByDateEnvoiAsc(conv.getIdConversation());
        if (!msgs.isEmpty()) {
            Message dernier = msgs.get(msgs.size() - 1);
            dto.setDernierMessage(dernier.getContenu());
            dto.setDateDernierMessage(dernier.getDateEnvoi());
        }
        dto.setNonLus(messageRepository.countByIdConversationAndStatutLectureAndIdExpediteurNot(
            conv.getIdConversation(), "ENVOYE", userId));

        return dto;
    }

    private UUID getUtilisateurId(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié.");
        Utilisateur u = utilisateurRepository.findByEmail(principal.getName().toLowerCase().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        return u.getIdUtilisateur();
    }
}
