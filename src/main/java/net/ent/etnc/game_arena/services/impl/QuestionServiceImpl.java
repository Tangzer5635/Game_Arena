package net.ent.etnc.game_arena.services.impl;

import net.ent.etnc.game_arena.models.entities.Question;
import net.ent.etnc.game_arena.models.entities.Reponse;
import net.ent.etnc.game_arena.repositories.QuestionRepository;
import net.ent.etnc.game_arena.services.QuestionService;
import net.ent.etnc.game_arena.services.ReponseService;
import net.ent.etnc.game_arena.services.commons.AbstractService;
import net.ent.etnc.game_arena.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuestionServiceImpl extends AbstractService<Question, QuestionRepository> implements QuestionService {

    private final ReponseService reponseService;

    @Autowired
    public QuestionServiceImpl(QuestionRepository questionRepository, ReponseService reponseService) {
        super(questionRepository);
        this.reponseService = reponseService;
    }

    @Override
    @Transactional
    public Question addReponses(Long questionId, List<Long> reponseIds) {
        Question question = repository.findById(questionId)
                .orElseThrow(() -> new ServiceException("La question avec l'ID " + questionId + " n'existe pas."));
        reponseIds.forEach(id -> {
            Reponse reponse = reponseService.findById(id).orElseThrow(() ->
                            new ServiceException("La réponse avec l'ID " + id + " n'existe pas."));
            question.addReponse(reponse);
        });

        return question;
    }

    @Override
    @Transactional
    public Question removeReponse(Long questionId, Long reponseId) {
        Question question = repository.findById(questionId)
                .orElseThrow(() ->
                        new ServiceException("La question avec l'ID " + questionId + " n'existe pas."));
        Reponse reponse = reponseService.findById(reponseId)
                .orElseThrow(() ->
                        new ServiceException("La réponse avec l'ID " + reponseId + " n'existe pas."));
        question.removeReponse(reponse);
        return question;
    }
}