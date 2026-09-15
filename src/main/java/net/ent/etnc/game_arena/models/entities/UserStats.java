package net.ent.etnc.game_arena.models.entities;

import jakarta.persistence.*;
import lombok.*;
import net.ent.etnc.game_arena.models.commons.AbstractPersistableWithIdSetter;

/**
 * Statistiques persistées d'un joueur.
 * Une ligne par utilisateur, mise à jour à la fin de chaque partie.
 * Ces données survivent aux redémarrages (stockées en base).
 */
@Entity
@Table(name = "USER_STATS")
@Getter
@Setter
@NoArgsConstructor
public class UserStats extends AbstractPersistableWithIdSetter<Long> {

    /**
     * Relation 1-1 avec l'utilisateur.
     * Cascade NONE : on ne supprime pas le User si on supprime ses stats.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** Nombre total de parties jouées jusqu'au bout. */
    @Column(nullable = false)
    private int partiesJouees = 0;

    /** Nombre total de bonnes réponses sur toutes les parties. */
    @Column(nullable = false)
    private int bonnesReponses = 0;

    /** Nombre total de mauvaises réponses. */
    @Column(nullable = false)
    private int mauvaisesReponses = 0;

    /** Score cumulé sur toutes les parties. */
    @Column(nullable = false)
    private int scoreCumule = 0;

    /** Meilleur score réalisé en une seule partie. */
    @Column(nullable = false)
    private int meilleurScore = 0;

    /** Meilleure série de bonnes réponses consécutives toutes parties confondues. */
    @Column(nullable = false)
    private int meilleureStreak = 0;

    // ── Méthodes métier ──────────────────────────────────────────────────────

    /**
     * Enregistre les résultats d'une partie terminée.
     *
     * @param scorePartie        score total obtenu dans cette partie
     * @param bonnes             nombre de bonnes réponses dans cette partie
     * @param mauvaises          nombre de mauvaises réponses dans cette partie
     * @param streakMax          meilleure streak atteinte dans cette partie
     */
    public void enregistrerPartie(int scorePartie, int bonnes, int mauvaises, int streakMax) {
        this.partiesJouees++;
        this.bonnesReponses += bonnes;
        this.mauvaisesReponses += mauvaises;
        this.scoreCumule += scorePartie;

        if (scorePartie > this.meilleurScore) {
            this.meilleurScore = scorePartie;
        }
        if (streakMax > this.meilleureStreak) {
            this.meilleureStreak = streakMax;
        }
    }

    /**
     * Taux de bonnes réponses, entre 0.0 et 1.0.
     * Retourne 0.0 si aucune réponse n'a encore été donnée.
     */
    public double tauxReussite() {
        int total = bonnesReponses + mauvaisesReponses;
        return total == 0 ? 0.0 : (double) bonnesReponses / total;
    }
}
