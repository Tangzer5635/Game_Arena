package net.ent.etnc.game_arena.repositories;

import net.ent.etnc.game_arena.models.entities.RefreshToken;
import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.repositories.commons.BaseRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends BaseRepository<RefreshToken> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Révoque tous les tokens d'une famille (détection de vol).
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.family = :family")
    void revokeAllByFamily(@Param("family") String family);

    /**
     * Révoque tous les tokens actifs d'un utilisateur (logout global).
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
    void revokeAllActiveByUser(@Param("user") User user);

    /**
     * Nettoyage planifiable : supprime les tokens expirés.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteExpiredBefore(@Param("now") Instant now);
}
