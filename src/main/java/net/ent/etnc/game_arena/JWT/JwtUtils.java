package net.ent.etnc.game_arena.JWT;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import net.ent.etnc.game_arena.models.entities.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

/**
 * Utilitaire JWT : génération et validation des access tokens.
 *
 * <p>Lit la clé secrète et la durée d'expiration depuis {@code application.yaml}.
 * La clé doit faire au moins 64 caractères pour HMAC-SHA256.
 *
 * <p>Deux méthodes de génération :
 * <ul>
 *   <li>{@link #generateJwtToken(Authentication)} — utilisée lors du login,
 *       à partir de l'Authentication Spring Security ;</li>
 *   <li>{@link #generateJwtTokenForUser(User)} — utilisée lors du refresh,
 *       sans Authentication complète.</li>
 * </ul>
 */
@Component
public class JwtUtils {

    @Value("${app.security.jwt.secret}")
    private String secret;

    @Value("${app.security.jwt.expiration:90000}")
    private long jwtExpirationMs;

    /**
     * Génère un JWT à partir d'une Authentication Spring Security.
     * Utilisé par {@code AuthController.login()}.
     */
    public String generateJwtToken(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return generateJwtTokenForUser(user);
    }

    /**
     * Génère un JWT directement à partir d'un User.
     * Utilisé par {@code RefreshTokenServiceImpl} lors de la rotation.
     */
    public String generateJwtTokenForUser(User user) {
        return Jwts.builder()
                .subject(user.getUsername())                    // claim "sub"
                .issuedAt(new Date())                           // claim "iat"
                .expiration(new Date(                           // claim "exp"
                        System.currentTimeMillis() + jwtExpirationMs))
                .claim("role", user.getRole().name())           // custom claim
                .signWith(getSecretKey())                       // HMAC-SHA256
                .compact();
    }

    /**
     * Valide le token et retourne le claim {@code sub} (username) s'il est valide.
     *
     * @param token le JWT brut (sans "Bearer ")
     * @return un Optional contenant le username, ou vide si le token est invalide ou expiré
     */
    public Optional<String> getUsernameIfValid(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims.getSubject());
        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    /** Construit la SecretKey HMAC-SHA256 à partir de la clé secrète en clair. */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
