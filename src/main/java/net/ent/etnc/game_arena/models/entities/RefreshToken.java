package net.ent.etnc.game_arena.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.ent.etnc.game_arena.models.commons.AbstractPersistableWithIdSetter;

import java.time.Instant;

/**
 * Refresh token stateful pour la rotation sécurisée des sessions.
 * <p>
 * Le token brut (UUID) n'est jamais stocké en base — seul son hash SHA-256 l'est.
 * Chaque utilisation invalide le token courant et en génère un nouveau (rotation).
 * <p>
 * Champ family : UUID partagé par tous les tokens d'une même chaîne de rotation.
 * Si un token déjà révoqué est présenté, toute la famille est révoquée → détection de vol.
 * <p>
 * Champs ip/userAgent : stockés à titre d'audit. Une incohérence déclenche un warning
 * sans bloquer (réseau mobile, VPN) — la rotation est la défense principale.
 */
@Entity
@Table(name = "refresh_tokens",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"token_hash"},
                name = "uk__refresh_tokens__token_hash"
        ),
        indexes = {
        @Index(name = "idx__refresh_tokens__token_hash", columnList = "token_hash"),
        @Index(name = "idx__refresh_tokens__family", columnList = "family"),
        @Index(name = "idx__refresh_tokens__user_id", columnList = "user_id"),
})
@ToString(callSuper = true, of = {"family", "revoked"})
public class RefreshToken extends AbstractPersistableWithIdSetter<Long> {

    /**
     * Hash SHA-256 (hex 64 chars) du token brut — jamais le token en clair.
     */
    @Getter
    @Setter
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk__refresh_tokens__users"))
    private User user;

    /**
     * Expiration glissante : réinitialisée à chaque rotation.
     */
    @Getter
    @Setter
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Date de création de ce token (fixe, jamais modifiée).
     */
    @Getter
    @Setter
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * True si le token a été consommé (rotation) ou révoqué (logout/vol).
     */
    @Getter
    @Setter
    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    /**
     * User-Agent lors de l'émission — audit uniquement.
     */
    @Getter
    @Setter
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /**
     * UUID partagé par toute la chaîne de rotation (login → refresh → refresh → ...).
     */
    @Getter
    @Setter
    @Column(name = "family", nullable = false, length = 36)
    private String family;
}
