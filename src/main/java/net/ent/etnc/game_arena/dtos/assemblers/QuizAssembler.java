package net.ent.etnc.game_arena.dtos.assemblers;

import net.ent.etnc.game_arena.dtos.QuizDto;
import net.ent.etnc.game_arena.models.entities.Quiz;
import net.ent.etnc.game_arena.services.QuizService;
import net.ent.etnc.game_arena.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QuizAssembler {

    private final QuizService quizService;

    @Autowired
    public QuizAssembler(QuizService quizService) {
        this.quizService = quizService;
    }

}