package net.ent.etnc.game_arena.controllers.handlers;

import net.ent.etnc.jurassicpark.exceptions.RefreshTokenExpiredException;
import net.ent.etnc.jurassicpark.exceptions.RefreshTokenNotFoundException;
import net.ent.etnc.jurassicpark.exceptions.RefreshTokenTheftException;
import net.ent.etnc.jurassicpark.services.commons.ServiceException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Gestionnaire global d'exceptions.
 *
 * Intercepte toutes les exceptions non gérées dans les controllers et les convertit
 * en réponses HTTP lisibles par le client.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /** Violations de règles métier → 400 Bad Request. */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<String> handleServiceException(ServiceException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    /** Identifiants incorrects au login → 401 Unauthorized. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(401).body("Identifiants incorrects.");
    }

    /** Refresh token introuvable en base → 401. */
    @ExceptionHandler(RefreshTokenNotFoundException.class)
    public ResponseEntity<String> handleRefreshNotFound(RefreshTokenNotFoundException e) {
        return ResponseEntity.status(401).body(e.getMessage());
    }

    /**
     * Refresh token révoqué présenté → vol probable → 401.
     * La famille a déjà été révoquée côté service.
     */
    @ExceptionHandler(RefreshTokenTheftException.class)
    public ResponseEntity<String> handleRefreshTheft(RefreshTokenTheftException e) {
        return ResponseEntity.status(401).body(e.getMessage());
    }

    /** Refresh token expiré → 401, le client doit se reconnecter. */
    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<String> handleRefreshExpired(RefreshTokenExpiredException e) {
        return ResponseEntity.status(401).body(e.getMessage());
    }

    /** Capture toutes les autres exceptions inattendues → 500. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        return ResponseEntity.internalServerError().body(e.getMessage());
    }
}
