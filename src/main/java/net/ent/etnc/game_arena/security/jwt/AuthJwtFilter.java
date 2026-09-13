package net.ent.etnc.game_arena.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre JWT : intercepte chaque requête HTTP pour vérifier le token d'authentification.
 *
 * Flux d'exécution pour chaque requête :
 *   1. Extraction du token depuis le header "Authorization: Bearer {token}"
 *   2. Validation du token (signature + expiration)
 *   3. Extraction du username depuis le token
 *   4. Chargement de l'utilisateur depuis la BDD
 *   5. Injection de l'authentification dans le SecurityContext
 *
 * OncePerRequestFilter garantit que le filtre n'est exécuté qu'une seule fois par requête.
 * Si le token est absent ou invalide, la requête continue sans authentification
 * → Spring Security retournera 401 si la route est protégée.
 */
@Component
public class AuthJwtFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Autowired
    public AuthJwtFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = getJwtFromRequest(request);

            // Si un token valide est présent, on authentifie l'utilisateur
            if (token != null && jwtUtils.validateJwtToken(token)) {
                String username = jwtUtils.getUsernameFromJwtToken(token);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // En cas d'erreur (token invalide, user supprimé...) on laisse passer la requête
            // Spring Security gérera le 401 si la route est protégée
        } finally {
            // TOUJOURS continuer la chaîne de filtres, même en cas d'erreur
            filterChain.doFilter(request, response);
        }
    }

    /** Extrait le token JWT depuis le header Authorization (format "Bearer {token}"). */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.trim().startsWith("Bearer ")) {
            return bearerToken.trim().substring(7);
        }
        return null;
    }
}
