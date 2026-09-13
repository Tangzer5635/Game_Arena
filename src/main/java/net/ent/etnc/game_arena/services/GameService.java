package net.ent.etnc.game_arena.services;

import net.ent.etnc.game_arena.dtos.GameAnswerResultDto;
import net.ent.etnc.game_arena.dtos.GameQuestionDto;

public interface GameService {

    /** Charge le quiz et envoie la première question à tous les joueurs. */
    void startGame(String code);

    /** Un joueur répond. Retourne son résultat individuel. */
    GameAnswerResultDto answer(String code, Long userId, Long reponseId);

    /** Passe à la question suivante (ou termine la partie). */
    void nextQuestion(String code);
}
