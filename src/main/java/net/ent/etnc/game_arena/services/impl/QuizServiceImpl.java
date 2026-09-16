package net.ent.etnc.game_arena.services.impl;

import net.ent.etnc.game_arena.models.entities.Question;
import net.ent.etnc.game_arena.models.entities.Quiz;
import net.ent.etnc.game_arena.repositories.QuizRepository;
import net.ent.etnc.game_arena.services.QuestionService;
import net.ent.etnc.game_arena.services.QuizService;
import net.ent.etnc.game_arena.services.commons.AbstractService;
import net.ent.etnc.game_arena.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuizServiceImpl
        extends AbstractService<Quiz, QuizRepository>
        implements QuizService {

    private final QuestionService questionService;

    @Autowired
    public QuizServiceImpl(QuizRepository quizRepository, QuestionService questionService) {
        super(quizRepository);
        this.questionService = questionService;
    }

    @Transactional(readOnly = true)
    public Quiz getQuizForGame(Long id) {
        return this.repository.findByIdWithQuestions(id)
                .orElseThrow(() -> new RuntimeException("Quiz introuvable"));
    }

    @Override
    public Quiz addQuestions(Long quizId, List<Long> questionIds) {
        Quiz quiz = findById(quizId)
                .orElseThrow(() ->
                        new ServiceException("Le quiz avec l'ID " + quizId + " n'existe pas."));

        for (Long questionId : questionIds) {
            Question question = questionService.findById(questionId)
                    .orElseThrow(() -> new ServiceException("La question avec l'ID " + questionId + " n'existe pas."));
            quiz.addQuestion(question);
        }
        return update(quiz);
    }

    @Override
    @Transactional
    public Quiz removeQuestion(Long quizId, Long questionId) {
        Quiz quiz = repository.findById(quizId)
                .orElseThrow(() ->
                        new ServiceException("Le quiz avec l'ID " + quizId + " n'existe pas."));
        Question question = questionService.findById(questionId)
                .orElseThrow(() ->
                        new ServiceException("La question avec l'ID " + questionId + " n'existe pas."));
        quiz.removeQuestion(question);
        return quiz;
    }
}