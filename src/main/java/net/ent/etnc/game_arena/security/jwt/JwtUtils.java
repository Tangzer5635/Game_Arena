package net.ent.etnc.game_arena.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import net.ent.etnc.game_arena.models.entities.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utilitaire JWT : génération, validation et parsing des tokens.
 *
 * Algorithme : HS256 (HMAC-SHA256) avec clé secrète.
 * La clé doit faire au moins 64 caractères (512 bits) pour HS512,
 * ou 32 caractères (256 bits) pour HS256.
 *
 * Contenu du token (claims) :
 *   - subject : username
 *   - email   : adresse email de l'utilisateur
 *   - role    : rôle (ADMIN)
 *   - iat     : date d'émission
 *   - exp     : date d'expiration
 */
@Component
public class JwtUtils {

    @Value("${app.security.jwt.secret:default-secret-key-please-change-in-production-32chars}")
    private String secret;

    @Value("${app.security.jwt.expiration:900000}")
    private long jwtExpirationMs;

    /**
     * Génère un token JWT après authentification réussie.
     * Appelé par AuthController.login() après validation des credentials.
     */
    public String generateJwtToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = (User) userDetails;

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + this.jwtExpirationMs))
                .claim("id", user.getId())
                .claim("role", user.getRole().name())
                .signWith(this.getSecretKey())
                .compact();
    }

    /**
     * Génère un token JWT directement depuis une entité User.
     * Utilisé par AuthController.refresh() (pas d'Authentication Spring disponible).
     */
    public String generateJwtTokenForUser(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + this.jwtExpirationMs))
                .claim("id", user.getId())
                .claim("role", user.getRole().name())
                .signWith(this.getSecretKey())
                .compact();
    }

    /**
     * Valide un token JWT (signature + expiration).
     * Retourne false si le token est null, invalide ou expiré.
     */
    public boolean validateJwtToken(String token) {
        if (token == null || token.isBlank()) return false;
        try {
            Jwts.parser()
                    .verifyWith(this.getSecretKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            // Token invalide ou expiré — on retourne false sans lever d'exception
            return false;
        }
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }


    /**
     * Extrait le username (subject) d'un token JWT valide.
     */
    public String getUsernameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(this.getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /** Construit la clé de signature HMAC à partir de la chaîne secrète. */
    private SecretKey getSecretKey() {
        byte[] encodedKey = this.secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(encodedKey);
    }
}
