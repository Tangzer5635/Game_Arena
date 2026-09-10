package net.ent.etnc.game_arena.services.impl;

import net.ent.etnc.game_arena.models.entities.Reponse;
import net.ent.etnc.game_arena.repositories.ReponseRepository;
import net.ent.etnc.game_arena.services.ReponseService;
import net.ent.etnc.game_arena.services.commons.AbstractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReponseServiceImpl extends AbstractService<Reponse, ReponseRepository> implements ReponseService {

    @Autowired
    public ReponseServiceImpl(ReponseRepository reponseRepository) {
        super(reponseRepository);
    }
}