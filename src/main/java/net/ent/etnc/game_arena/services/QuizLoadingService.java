package net.ent.etnc.game_arena.services;

import lombok.RequiredArgsConstructor;
import net.ent.etnc.game_arena.models.entities.Question;
import net.ent.etnc.game_arena.models.entities.Quiz;
import net.ent.etnc.game_arena.repositories.QuestionRepository;
import net.ent.etnc.game_arena.repositories.QuizRepository;
import net.ent.etnc.game_arena.services.commons.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizLoadingService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;

    @Transactional(readOnly = true)
    public Quiz loadQuiz(Long quizId) {

        Quiz quiz = quizRepository.findByIdWithQuestions(quizId)
                .orElseThrow(() -> new ServiceException("Quiz introuvable."));

        List<Long> questionIds = quiz.getQuestions()
                .stream()
                .map(Question::getId)
                .toList();

        if (!questionIds.isEmpty()) {
            questionRepository.findAllByIdWithReponses(questionIds);

            // Force l'initialisation pendant que la transaction est ouverte
            quiz.getQuestions().forEach(question ->
                    question.getReponses().size()
            );
        }

        return quiz;
    }
}