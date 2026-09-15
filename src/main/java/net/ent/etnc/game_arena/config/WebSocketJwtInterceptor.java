package net.ent.etnc.game_arena.config;

import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.security.jwt.JwtUtils;
import net.ent.etnc.game_arena.services.UserService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class WebSocketJwtInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;
    private final UserService userService;

    public WebSocketJwtInterceptor(JwtUtils jwtUtils, UserService userService) {
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        // getNativeAccessor() permet de MODIFIER les headers (wrap() est en lecture seule)
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        // ── CONNECT : lit le JWT et positionne le Principal sur la SESSION ──
        // Le Principal stocké sur la session est automatiquement propagé
        // à tous les SEND suivants de la même connexion STOMP.
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authorization = accessor.getFirstNativeHeader("Authorization");

            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.substring(7);

                try {
                    if (jwtUtils.validateJwtToken(token)) {

                        String username = jwtUtils.getUserNameFromJwtToken(token);
                        User user = userService.findByUsername(username);

                        if (user != null) {
                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(
                                            user, null, user.getAuthorities()
                                    );

                            // Positionne le Principal sur la SESSION STOMP
                            accessor.setUser(auth);

                            System.out.println("WebSocket CONNECT authentifié : "
                                    + user.getUsername() + " (id=" + user.getId() + ")");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("WebSocket CONNECT : JWT invalide — " + e.getMessage());
                }
            }
        }

        return message;
    }
}
