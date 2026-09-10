package net.ent.etnc.game_arena.exceptions;

/**
 * Lancée quand un refresh token déjà révoqué est présenté — signe probable de vol.
 * Déclenche la révocation de toute la famille de tokens.
 */
public class RefreshTokenTheftException extends RuntimeException {
    public RefreshTokenTheftException() {
        super("Token révoqué présenté : vol détecté. Toute la session est invalidée.");
    }
}
