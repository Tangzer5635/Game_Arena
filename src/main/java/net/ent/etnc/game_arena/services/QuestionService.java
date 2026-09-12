package net.ent.etnc.game_arena.services;

import net.ent.etnc.game_arena.models.entities.Question;
import net.ent.etnc.game_arena.services.commons.Service;

import java.util.List;

public interface QuestionService extends Service<Question, Long> {

    Question addReponses(Long questionId, List<Long> reponseIds);

    Question removeReponse(Long questionId, Long reponseId);

}