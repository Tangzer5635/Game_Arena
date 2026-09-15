package net.ent.etnc.game_arena.models.entities;

import jakarta.persistence.*;
import lombok.*;
import net.ent.etnc.game_arena.models.commons.AbstractPersistableWithIdSetter;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * État live d'une partie en cours, persisté en base PostgreSQL.
 * <p>
 * Remplace les ConcurrentHashMap en RAM de l'ancienne architecture.
 * Avantages :
 * <ul>
 *   <li>Survit aux redémarrages du backend (essentiel pour la conteneurisation)</li>
 *   <li>Cohérence garantie par JPA + transactions</li>
 *   <li>Nettoyable via TTL par {@code SalonTtlScheduler}</li>
 * </ul>
 * Une ligne par partie active, supprimée à la fin.
 */
@Entity
@Table(name = "GAME_STATE")
@Getter
@Setter
@NoArgsConstructor
public class GameState extends AbstractPersistableWithIdSetter<Long> {

    /** Code du salon auquel cet état est rattaché (unicité garantie : une partie par salon). */
    @Column(nullable = false, unique = true, length = 4)
    private String salonCode;

    /** ID du quiz en cours. */
    @Column(nullable = false)
    private Long quizId;

    /** Index (0-based) de la question actuellement posée. -1 = compte à rebours en cours. */
    @Column(nullable = false)
    private int currentQuestionIndex = -1;

    /** Timestamp serveur (epoch ms) du début de la question courante — synchronise le timer front. */
    @Column(nullable = false)
    private long questionStartedAt = 0L;

    /**
     * Scores par userId, sérialisés en JSON par Hibernate (via @ElementCollection).
     * Clé : userId (Long), Valeur : score (Integer).
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "GAME_SCORES", joinColumns = @JoinColumn(name = "game_state_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "score")
    private Map<Long, Integer> scores = new HashMap<>();

    /**
     * Streaks courants par userId (réinitialisés à 0 sur mauvaise réponse).
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "GAME_STREAKS", joinColumns = @JoinColumn(name = "game_state_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "streak")
    private Map<Long, Integer> streaks = new HashMap<>();

    /**
     * Indique si le bonus ×2 est activé (streak ≥ 3) par userId.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "GAME_STREAK_BONUSES", joinColumns = @JoinColumn(name = "game_state_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "bonus_active")
    private Map<Long, Boolean> streakBonuses = new HashMap<>();

    /**
     * IDs des joueurs ayant déjà répondu à la question courante.
     * Vidé à chaque nouvelle question via {@code clearAnswers()}.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "GAME_ANSWERED", joinColumns = @JoinColumn(name = "game_state_id"))
    @Column(name = "user_id")
    private Set<Long> answeredUserIds = new HashSet<>();

    /** Timestamp de dernière activité — utilisé par le TTL scheduler pour nettoyer les parties abandonnées. */
    @Column(nullable = false)
    private Instant lastActivityAt = Instant.now();

    // ── Méthodes métier ──────────────────────────────────────────────────────

    /**
     * Initialise les scores/streaks pour tous les joueurs d'un salon.
     * À appeler une seule fois au démarrage de la partie.
     *
     * @param userIds liste des IDs des joueurs participant à la partie
     */
    public void initForPlayers(java.util.List<Long> userIds) {
        scores.clear();
        streaks.clear();
        streakBonuses.clear();
        for (Long uid : userIds) {
            scores.put(uid, 0);
            streaks.put(uid, 0);
            streakBonuses.put(uid, false);
        }
        touch();
    }

    /**
     * Ajoute des points au score d'un joueur.
     *
     * @param userId ID du joueur
     * @param points points à ajouter (toujours positif ou nul)
     */
    public void addScore(Long userId, int points) {
        scores.merge(userId, points, Integer::sum);
        touch();
    }

    /**
     * Marque un joueur comme ayant répondu à la question courante.
     * Retourne {@code false} si le joueur avait déjà répondu (double soumission).
     */
    public boolean markAnswered(Long userId) {
        boolean added = answeredUserIds.add(userId);
        if (added) touch();
        return added;
    }

    /**
     * Indique si tous les joueurs inscrits ont répondu.
     * Déclenche le reveal automatique quand c'est le cas.
     */
    public boolean allAnswered() {
        return answeredUserIds.size() >= scores.size();
    }

    /** Vide la liste des répondants pour préparer la question suivante. */
    public void clearAnswers() {
        answeredUserIds.clear();
        touch();
    }

    /** Met à jour le timestamp de dernière activité (utilisé par le TTL). */
    public void touch() {
        this.lastActivityAt = Instant.now();
    }
}
