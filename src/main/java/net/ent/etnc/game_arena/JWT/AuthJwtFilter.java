package net.ent.etnc.game_arena.JWT;

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
 * Filtre JWT : intercepte chaque requête HTTP, extrait et valide le token Bearer,
 * puis alimente le SecurityContext si le token est valide.
 *
 * <p>Hérite de {@link OncePerRequestFilter} — garanti d'être exécuté exactement
 * une fois par requête, même si la chaîne de filtres est traversée plusieurs fois.
 *
 * <p>Algorithme :
 * <ol>
 *   <li>Extraire le token depuis le header {@code Authorization: Bearer <token>}</li>
 *   <li>Si absent ou malformé → passer au filtre suivant (Spring Security rejettera si la route est protégée)</li>
 *   <li>Valider via {@link JwtUtils#getUsernameIfValid(String)}</li>
 *   <li>Si valide → charger l'utilisateur depuis la base et remplir le SecurityContext</li>
 *   <li>Toujours appeler {@code chain.doFilter()} — c'est Spring Security qui décide ensuite</li>
 * </ol>
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
                                    FilterChain chain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            jwtUtils.getUsernameIfValid(token).ifPresent(username -> {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }

        chain.doFilter(request, response);
    }

    /** Extrait le token brut depuis le header Authorization (sans le préfixe "Bearer "). */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
