package com.colisender.api.service;
 import java.util.UUID;
import com.colisender.api.model.Colis;
import com.colisender.api.repository.ColisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
 

@Service
public class ColisService {
 
    @Autowired
    private ColisRepository colisRepository;
 
    // CORRECTION: types Long → UUID
    public void deleteColis(UUID id, UUID utilisateurId) {
        Colis colis = colisRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis non trouvé"));
 
        if (!colis.getIdUtilisateur().equals(utilisateurId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé.");
        }
        if (!"EN_ATTENTE".equals(colis.getStatutColis())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de supprimer un colis en cours d'acheminement.");
        }
 
        colisRepository.delete(colis);
    }
 
    // CORRECTION: types Long → UUID + typo villeArrivee → villeArrive
    public Colis updateColis(UUID id, Colis colisDetails, UUID utilisateurId) {
        Colis colis = colisRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis non trouvé"));
 
        if (!colis.getIdUtilisateur().equals(utilisateurId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé.");
        }
        if (!"EN_ATTENTE".equals(colis.getStatutColis())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce colis ne peut plus être modifié.");
        }
 
        colis.setDescription(colisDetails.getDescription());
        colis.setPoids(colisDetails.getPoids());
        colis.setDimension(colisDetails.getDimension());
        colis.setVilleDepart(colisDetails.getVilleDepart());
        colis.setVilleArrive(colisDetails.getVilleArrive()); // CORRECTION: villeArrivee → villeArrive
        colis.setAdresseRecuperation(colisDetails.getAdresseRecuperation());
        colis.setAdresseLivraison(colisDetails.getAdresseLivraison());
        colis.setPrixTransport(colisDetails.getPrixTransport());
        colis.setTelephoneDestinataire(colisDetails.getTelephoneDestinataire());
 
        return colisRepository.save(colis);
    }
}