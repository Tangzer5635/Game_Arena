package net.ent.etnc.game_arena.dtos.assemblers;

import net.ent.etnc.game_arena.dtos.QuestionDto;
import net.ent.etnc.game_arena.models.entities.Question;
import net.ent.etnc.game_arena.services.QuestionService;
import net.ent.etnc.game_arena.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QuestionAssembler {

    private final QuestionService questionService;

    @Autowired
    public QuestionAssembler(QuestionService questionService) {
        this.questionService = questionService;
    }

}