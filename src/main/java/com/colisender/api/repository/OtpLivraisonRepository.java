package com.colisender.api.repository;

import com.colisender.api.model.OtpLivraison;
import com.colisender.api.model.StatutOtpEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OtpLivraisonRepository extends JpaRepository<OtpLivraison, UUID> {

    // Récupérer l'OTP actif pour un colis
    Optional<OtpLivraison> findByIdColisAndStatut(UUID idColis, StatutOtpEnum statut);

    // Version tolérante aux doublons — prend le plus récent
    List<OtpLivraison> findByIdColisAndStatutOrderByDateExpirationDesc(UUID idColis, StatutOtpEnum statut);

    // Expire tous les OTP actifs d'un colis sauf celui à conserver
    @Transactional
    @Modifying
    @Query("UPDATE OtpLivraison o SET o.statut = 'EXPIRE' WHERE o.idColis = :idColis AND o.statut = 'ACTIF' AND o.id <> :idAConserver")
    void expirerAutresOtpsActifs(@Param("idColis") UUID idColis, @Param("idAConserver") UUID idAConserver);

    // Expire tous les OTP actifs d'un colis (quand livraison validée)
    @Transactional
    @Modifying
    @Query("UPDATE OtpLivraison o SET o.statut = 'UTILISE' WHERE o.idColis = :idColis AND o.statut = 'ACTIF'")
    void marquerUtilise(@Param("idColis") UUID idColis);
}
