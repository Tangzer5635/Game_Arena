package net.ent.etnc.game_arena.services;

import net.ent.etnc.game_arena.dtos.UserStatsDto;

/**
 * Service exposant les statistiques de profil d'un joueur.
 */
public interface UserStatsService {

    /**
     * Retourne les stats du joueur identifié par {@code userId}.
     * Si le joueur n'a pas encore joué, retourne des stats vierges (tous à 0).
     */
    UserStatsDto getStats(Long userId);
}
