package net.ent.etnc.game_arena.services.impl;

import net.ent.etnc.game_arena.models.entities.Quiz;
import net.ent.etnc.game_arena.repositories.QuizRepository;
import net.ent.etnc.game_arena.services.QuizService;
import net.ent.etnc.game_arena.services.commons.AbstractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuizServiceImpl extends AbstractService<Quiz, QuizRepository> implements QuizService {

    @Autowired
    public QuizServiceImpl(QuizRepository quizRepository) {
        super(quizRepository);
    }
}