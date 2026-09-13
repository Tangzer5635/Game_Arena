package net.ent.etnc.game_arena.services;

import net.ent.etnc.game_arena.models.entities.Salon;
import net.ent.etnc.game_arena.models.enumerations.EtatSalon;
import net.ent.etnc.game_arena.services.commons.Service;

public interface SalonService {

    Salon create(Long userId);

    Salon findByCode(String code);

    Salon addUser(String code, Long userId);

    Salon removeUser(String code, Long userId);

    Salon changeEtat(String code, EtatSalon etat);

    Salon start(String code, Long userId, Long quizId);

    void delete(String code, Long userId);
}