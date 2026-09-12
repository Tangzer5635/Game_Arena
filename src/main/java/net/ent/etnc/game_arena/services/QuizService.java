package net.ent.etnc.game_arena.services;

import net.ent.etnc.game_arena.models.entities.Quiz;
import net.ent.etnc.game_arena.services.commons.Service;

import java.util.List;

public interface QuizService extends Service<Quiz, Long> {

    Quiz addQuestions(Long quizId, List<Long> questionIds);

    Quiz removeQuestion(Long quizId, Long questionId);

}