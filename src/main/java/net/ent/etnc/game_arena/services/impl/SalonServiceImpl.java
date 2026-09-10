package net.ent.etnc.game_arena.services.impl;

import net.ent.etnc.game_arena.models.entities.Salon;
import net.ent.etnc.game_arena.repositories.SalonRepository;
import net.ent.etnc.game_arena.services.SalonService;
import net.ent.etnc.game_arena.services.commons.AbstractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SalonServiceImpl extends AbstractService<Salon, SalonRepository> implements SalonService {

    @Autowired
    public SalonServiceImpl(SalonRepository salonRepository) {
        super(salonRepository);
    }
}