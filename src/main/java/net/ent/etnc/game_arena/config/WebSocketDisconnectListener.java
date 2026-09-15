package net.ent.etnc.game_arena.config;

import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.services.SalonService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * Éjecte automatiquement un joueur de son salon quand son WebSocket se ferme
 * (onglet fermé, réseau coupé, etc.).
 * Sans ça, un joueur déconnecté reste dans salon.getUsers() et bloque allAnswered().
 */
@Component
public class WebSocketDisconnectListener {

    private final SalonService salonService;

    public WebSocketDisconnectListener(SalonService salonService) {
        this.salonService = salonService;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();

        if (principal == null) return;

        // Le principal est un UsernamePasswordAuthenticationToken dont le détail est un User
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth) {
            if (auth.getPrincipal() instanceof User user) {
                // Cherche si ce joueur est dans un salon et le retire
                salonService.removeUserFromAnySalon(user.getId());
            }
        }
    }
}
