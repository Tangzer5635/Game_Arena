package net.ent.etnc.game_arena.exceptions;

/** Lancée quand le hash du refresh token n'existe pas en base. */
public class RefreshTokenNotFoundException extends RuntimeException {
    public RefreshTokenNotFoundException() {
        super("Refresh token introuvable.");
    }
}
