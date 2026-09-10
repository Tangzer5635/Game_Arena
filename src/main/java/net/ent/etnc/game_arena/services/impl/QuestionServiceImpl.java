package net.ent.etnc.game_arena.services.impl;

import net.ent.etnc.game_arena.models.entities.Question;
import net.ent.etnc.game_arena.repositories.QuestionRepository;
import net.ent.etnc.game_arena.services.QuestionService;
import net.ent.etnc.game_arena.services.commons.AbstractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionServiceImpl extends AbstractService<Question, QuestionRepository> implements QuestionService {

    @Autowired
    public QuestionServiceImpl(QuestionRepository questionRepository) {
        super(questionRepository);
    }
}