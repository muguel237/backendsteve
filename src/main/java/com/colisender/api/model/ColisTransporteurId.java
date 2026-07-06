package com.colisender.api.model;
 
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
 
public class ColisTransporteurId implements Serializable {
    private UUID idColis;
    private UUID idUtilisateur;
 
    public ColisTransporteurId() {}
 
    public ColisTransporteurId(UUID idColis, UUID idUtilisateur) {
        this.idColis = idColis;
        this.idUtilisateur = idUtilisateur;
    }
 
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColisTransporteurId)) return false;
        ColisTransporteurId that = (ColisTransporteurId) o;
        return Objects.equals(idColis, that.idColis) && Objects.equals(idUtilisateur, that.idUtilisateur);
    }
 
    @Override
    public int hashCode() { return Objects.hash(idColis, idUtilisateur); }
}