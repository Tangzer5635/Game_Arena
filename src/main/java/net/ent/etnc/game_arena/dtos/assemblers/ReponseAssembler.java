package net.ent.etnc.game_arena.dtos.assemblers;

import net.ent.etnc.game_arena.dtos.ReponseDto;
import net.ent.etnc.game_arena.models.entities.Reponse;
import net.ent.etnc.game_arena.services.ReponseService;
import net.ent.etnc.game_arena.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReponseAssembler {

    private final ReponseService reponseService;

    @Autowired
    public ReponseAssembler(ReponseService reponseService) {
        this.reponseService = reponseService;
    }

}