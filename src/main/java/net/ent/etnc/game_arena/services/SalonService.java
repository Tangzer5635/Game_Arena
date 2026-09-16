package net.ent.etnc.game_arena.services;

import net.ent.etnc.game_arena.models.entities.SalonEntity;
import net.ent.etnc.game_arena.models.enumerations.EtatSalon;

/**
 * Contrat du service gérant le cycle de vie des salons de jeu.
 * Toutes les implémentations doivent persister l'état en base (pas de RAM).
 */
public interface SalonService {

    SalonEntity create(Long userId, int maxPlayers);

    SalonEntity findByCode(String code);

    /** Charge le salon avec ses joueurs (JOIN FETCH) — à utiliser hors contexte transactionnel. */
    SalonEntity findByCodeWithUsers(String code);

    SalonEntity addUser(String code, Long userId);

    SalonEntity removeUser(String code, Long userId);

    SalonEntity changeEtat(String code, EtatSalon etat);

    SalonEntity start(String code, Long userId, Long quizId);

    void delete(String code, Long userId);

    /** Suppression en fin de partie (sans vérification de droits). */
    void cleanup(String code);

    /** Déconnexion WebSocket : retire le joueur de son salon actif. */
    void removeUserFromAnySalon(Long userId);
}
