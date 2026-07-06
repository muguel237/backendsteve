package com.colisender.api.repository;

import com.colisender.api.model.Otp;
import com.colisender.api.model.StatutOtpEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OtpRepository extends JpaRepository<Otp, UUID> {

    List<Otp> findByIdUtilisateurAndStatut(UUID idUtilisateur, StatutOtpEnum statut);

    // Réservé aux OTP d'authentification/inscription : exclut tout OTP de livraison
    // pour éviter de comparer par erreur un code de livraison à un code de connexion.
    List<Otp> findByIdUtilisateurAndStatutAndIdColisIsNull(UUID idUtilisateur, StatutOtpEnum statut);

    Optional<Otp> findByIdColisAndStatut(UUID idColis, StatutOtpEnum statut);

    // OTPs de livraison uniquement (idColis NOT NULL) — exclut les OTPs d'auth
    @Query("SELECT o FROM Otp o WHERE o.idColis = :idColis AND o.statut = :statut AND o.idColis IS NOT NULL ORDER BY o.dateExpiration DESC")
    List<Otp> findLivraisonOtpsByIdColisAndStatut(@Param("idColis") UUID idColis, @Param("statut") StatutOtpEnum statut);

    // Version tolérante aux doublons : si plusieurs OTP "ACTIF" existent pour le
    // même colis (ex: double appel concurrent lors du polling de paiement),
    // on prend le plus récent au lieu de planter avec IncorrectResultSizeDataAccessException.
    List<Otp> findByIdColisAndStatutOrderByDateExpirationDesc(UUID idColis, StatutOtpEnum statut);

    // Nettoyage : neutralise tous les OTP actifs superflus pour un colis,
    // en ne gardant que celui dont l'id est passé en paramètre.
    @Transactional
    @Modifying
    @Query("UPDATE Otp o SET o.statut = 'EXPIRE' WHERE o.idColis = :idColis AND o.statut = 'ACTIF' AND o.idCode <> :idCodeAConserver")
    void expirerAutresOtpsActifs(@Param("idColis") UUID idColis, @Param("idCodeAConserver") UUID idCodeAConserver);

    // IMPORTANT : ne cible QUE les OTP d'authentification/inscription (idColis == null).
    // Ne doit JAMAIS invalider un OTP de livraison (idColis renseigné), sinon le code
    // envoyé au destinataire est effacé silencieusement si ce même compte redemande
    // un OTP d'inscription/connexion pendant qu'une livraison est en cours.
    @Transactional
    @Modifying
    @Query("UPDATE Otp o SET o.statut = 'EXPIRE' WHERE o.idUtilisateur = :idUtilisateur AND o.statut = 'ACTIF' AND o.idColis IS NULL")
    void expireAllActiveOtpsForUser(@Param("idUtilisateur") UUID idUtilisateur);
}
