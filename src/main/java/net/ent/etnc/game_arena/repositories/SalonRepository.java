package net.ent.etnc.game_arena.repositories;

import net.ent.etnc.game_arena.models.entities.SalonEntity;
import net.ent.etnc.game_arena.models.enumerations.EtatSalon;
import net.ent.etnc.game_arena.repositories.commons.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalonRepository extends BaseRepository<SalonEntity> {

    Optional<SalonEntity> findByCode(String code);

    /** Charge le salon avec ses joueurs en un seul SELECT (évite LazyInitializationException hors transaction). */
    @Query("SELECT s FROM SalonEntity s LEFT JOIN FETCH s.users WHERE s.code = :code")
    Optional<SalonEntity> findByCodeWithUsers(String code);

    /** Vérifie si un utilisateur est déjà membre d'un salon actif. */
    @Query("SELECT COUNT(s) > 0 FROM SalonEntity s JOIN s.users u WHERE u.id = :userId AND s.etat != 'TERMINE'")
    boolean existsByUserIdAndNotTermine(Long userId);

    /** Renvoie le salon actif (non terminé) d'un utilisateur, s'il existe. */
    @Query("SELECT s FROM SalonEntity s JOIN s.users u WHERE u.id = :userId AND s.etat != 'TERMINE'")
    Optional<SalonEntity> findActiveSalonByUserId(Long userId);

    /**
     * Salons OUVERT inactifs depuis plus de {@code cutoff} — candidats à la suppression par le TTL.
     */
    List<SalonEntity> findByEtatAndLastActivityAtBefore(EtatSalon etat, Instant cutoff);
}
