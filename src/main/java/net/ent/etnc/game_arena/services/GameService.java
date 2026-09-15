package net.ent.etnc.game_arena.services;

public interface GameService {
    void startGame(String code, Long userId);
    void answer(String code, Long userId, Long reponseId, String answer);
    /** Renvoie la question courante au joueur qui vient de se reconnecter. */
    void rejoin(String code, Long userId);
}
