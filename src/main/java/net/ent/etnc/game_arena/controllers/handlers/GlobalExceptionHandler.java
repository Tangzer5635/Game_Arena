package net.ent.etnc.game_arena.controllers.handlers;

import jakarta.persistence.OptimisticLockException;
import net.ent.etnc.game_arena.services.commons.ServiceException;
import org.hibernate.StaleObjectStateException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Gestionnaire global d'exceptions.
 *
 * Intercepte toutes les exceptions non gérées dans les controllers et les convertit
 * en réponses HTTP lisibles par le client.
 *
 * @ControllerAdvice : Spring intercepte les exceptions de TOUS les controllers
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Intercepte les ServiceException (violations de règles métier).
     * Retourne un HTTP 500 avec le message d'erreur en texte brut.
     *
     * Note : Pour une API en production, on utiliserait un objet ErrorDto structuré
     * et un code HTTP adapté (400, 409, etc.). Pour le TP, un message String suffit.
     */
    /**
     * Intercepte les AccessDeniedException levées par @PreAuthorize (AOP/méthode).
     * Ces exceptions atteignent DispatcherServlet avant ExceptionTranslationFilter,
     * qui ne peut donc pas les convertir automatiquement en 401/403.
     * → utilisateur anonyme  : 401 (doit se connecter)
     * → utilisateur connecté : 403 (droits insuffisants)
     */
    /**
     * Conflit de version Hibernate (deux utilisateurs ont modifié le même quiz simultanément).
     * Retourne 409 Conflict avec un message explicite pour que le front puisse proposer
     * un rechargement.
     */
    @ExceptionHandler({
            ObjectOptimisticLockingFailureException.class,
            OptimisticLockException.class,
            StaleObjectStateException.class
    })
    public ResponseEntity<String> handleOptimisticLock(Exception e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Ce quiz a été modifié par quelqu'un d'autre. Rechargez la page pour voir la version à jour.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDeniedException(AccessDeniedException e) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié.");
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès interdit : droits insuffisants.");
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<String> handleServiceException(ServiceException e) {
        e.printStackTrace();
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    /** Capture toutes les autres exceptions inattendues. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        return ResponseEntity.internalServerError().body(e.getMessage());
    }
}
