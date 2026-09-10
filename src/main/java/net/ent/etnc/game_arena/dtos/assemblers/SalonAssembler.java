package net.ent.etnc.game_arena.dtos.assemblers;

import net.ent.etnc.game_arena.dtos.SalonDto;
import net.ent.etnc.game_arena.models.entities.Salon;
import net.ent.etnc.game_arena.services.SalonService;
import net.ent.etnc.game_arena.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SalonAssembler {

    private final SalonService salonService;

    @Autowired
    public SalonAssembler(SalonService salonService) {
        this.salonService = salonService;
    }

}