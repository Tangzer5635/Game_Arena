package net.ent.etnc.game_arena.dtos;

import lombok.*;

/**
 * DTO exposant les statistiques publiques d'un joueur.
 * Retourné par {@code GET /api/v1/users/{id}/stats/}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDto {

    private Long userId;
    private String username;

    /** Nombre de parties jouées jusqu'au bout. */
    private int partiesJouees;

    /** Score total cumulé sur toutes les parties. */
    private int scoreCumule;

    /** Meilleur score réalisé en une seule partie. */
    private int meilleurScore;

    /** Nombre de bonnes réponses toutes parties confondues. */
    private int bonnesReponses;

    /** Nombre de mauvaises réponses toutes parties confondues. */
    private int mauvaisesReponses;

    /**
     * Taux de réussite en pourcentage (0–100), arrondi à l'entier.
     * Calculé côté backend pour éviter la division par zéro côté client.
     */
    private int tauxReussitePct;

    /** Meilleure streak (bonnes réponses consécutives) toutes parties confondues. */
    private int meilleureStreak;
}
