package net.ent.etnc.game_arena.exceptions;

/** Lancée quand le refresh token est techniquement valide mais sa date d'expiration est passée. */
public class RefreshTokenExpiredException extends RuntimeException {
    public RefreshTokenExpiredException() {
        super("Refresh token expiré. Veuillez vous reconnecter.");
    }
}
