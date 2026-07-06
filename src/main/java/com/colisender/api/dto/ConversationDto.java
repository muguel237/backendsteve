package com.colisender.api.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ConversationDto {
    private UUID idConversation;
    private UUID idColis;
    private String typeConversation;
    private LocalDateTime dateCreation;
    private String villeDepart;
    private String villeArrive;
    private String statutColis;
    private List<ParticipantDto> participants;
    private String dernierMessage;
    private LocalDateTime dateDernierMessage;
    private long nonLus;

    public UUID getIdConversation() { return idConversation; }
    public void setIdConversation(UUID idConversation) { this.idConversation = idConversation; }

    public UUID getIdColis() { return idColis; }
    public void setIdColis(UUID idColis) { this.idColis = idColis; }

    public String getTypeConversation() { return typeConversation; }
    public void setTypeConversation(String typeConversation) { this.typeConversation = typeConversation; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public String getVilleDepart() { return villeDepart; }
    public void setVilleDepart(String villeDepart) { this.villeDepart = villeDepart; }

    public String getVilleArrive() { return villeArrive; }
    public void setVilleArrive(String villeArrive) { this.villeArrive = villeArrive; }

    public String getStatutColis() { return statutColis; }
    public void setStatutColis(String statutColis) { this.statutColis = statutColis; }

    public List<ParticipantDto> getParticipants() { return participants; }
    public void setParticipants(List<ParticipantDto> participants) { this.participants = participants; }

    public String getDernierMessage() { return dernierMessage; }
    public void setDernierMessage(String dernierMessage) { this.dernierMessage = dernierMessage; }

    public LocalDateTime getDateDernierMessage() { return dateDernierMessage; }
    public void setDateDernierMessage(LocalDateTime dateDernierMessage) { this.dateDernierMessage = dateDernierMessage; }

    public long getNonLus() { return nonLus; }
    public void setNonLus(long nonLus) { this.nonLus = nonLus; }

    public static class ParticipantDto {
        private UUID idUtilisateur;
        private String nom;
        private String prenom;
        private String photoProfil;
        private String role; // EXPEDITEUR | TRANSPORTEUR | DESTINATAIRE

        public UUID getIdUtilisateur() { return idUtilisateur; }
        public void setIdUtilisateur(UUID idUtilisateur) { this.idUtilisateur = idUtilisateur; }

        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }

        public String getPrenom() { return prenom; }
        public void setPrenom(String prenom) { this.prenom = prenom; }

        public String getPhotoProfil() { return photoProfil; }
        public void setPhotoProfil(String photoProfil) { this.photoProfil = photoProfil; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}
