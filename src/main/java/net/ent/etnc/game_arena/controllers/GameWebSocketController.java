package net.ent.etnc.game_arena.controllers;

import net.ent.etnc.game_arena.dtos.GameAnswerRequestDto;
import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.services.GameService;
import net.ent.etnc.game_arena.services.SalonService;
import net.ent.etnc.game_arena.services.UserService;
import net.ent.etnc.game_arena.services.commons.ServiceException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class GameWebSocketController {

    private final GameService gameService;
    private final UserService userService;
    private final SalonService salonService;

    public GameWebSocketController(GameService gameService, UserService userService, SalonService salonService) {
        this.gameService = gameService;
        this.userService = userService;
        this.salonService = salonService;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Résout le Principal STOMP en User et vérifie qu'il appartient au salon. */
    private User resolveAndVerify(String code, Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("Utilisateur WebSocket non authentifié.");
        }

        User user = userService.findByUsername(principal.getName());
        if (user == null) {
            throw new IllegalStateException("Utilisateur introuvable : " + principal.getName());
        }

        // Vérification de sécurité : le user doit être dans ce salon
        try {
            boolean isMember = salonService.findByCode(code)
                    .getUsers()
                    .stream()
                    .anyMatch(u -> u.getId().equals(user.getId()));

            if (!isMember) {
                throw new ServiceException(
                        "Accès refusé : " + user.getUsername() + " n'est pas membre du salon " + code
                );
            }
        } catch (ServiceException e) {
            // Le salon n'existe pas ou user pas membre → on laisse remonter
            throw e;
        }

        return user;
    }

    // ── Endpoints ────────────────────────────────────────────────────────────

    /**
     * L'hôte lance la partie.
     * Vérifie que le Principal est bien membre du salon avant d'appeler le service.
     */
    @MessageMapping("/game/{code}/start")
    public void startGame(@DestinationVariable String code, Principal principal) {
        User user = resolveAndVerify(code, principal);
        gameService.startGame(code, user.getId());
    }

    /**
     * Un joueur répond à la question courante.
     * L'userId est tiré du Principal STOMP (impossible à falsifier côté client)
     * et non du body de la requête — ce qui empêche l'usurpation d'identité.
     */
    @MessageMapping("/game/{code}/answer")
    public void answer(@DestinationVariable String code, GameAnswerRequestDto request, Principal principal) {
        User user = resolveAndVerify(code, principal);
        gameService.answer(code, user.getId(), request.getReponseId(), request.getAnswer());
    }

    /**
     * Reconnexion mi-partie : un joueur qui refresh se réabonne et demande
     * l'état courant. Le service lui renvoie la question en cours sur son topic.
     */
    @MessageMapping("/game/{code}/rejoin")
    public void rejoin(@DestinationVariable String code, Principal principal) {
        User user = resolveAndVerify(code, principal);
        gameService.rejoin(code, user.getId());
    }
}
