package net.ent.etnc.game_arena.services.impl;

import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.game_arena.models.entities.RefreshToken;
import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.repositories.RefreshTokenRepository;
import net.ent.etnc.game_arena.services.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository repository;

    @Value("${app.security.refresh-token.expiration-hours:2}")
    private int expirationHours;

    public RefreshTokenServiceImpl(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public String createToken(User user, String userAgent) {
        String rawToken = UUID.randomUUID().toString();
        String family = UUID.randomUUID().toString();

        RefreshToken token = new RefreshToken();
        token.setTokenHash(hash(rawToken));
        token.setUser(user);
        token.setExpiresAt(Instant.now().plus(expirationHours, ChronoUnit.HOURS));
        token.setCreatedAt(Instant.now());
        token.setRevoked(false);
        token.setUserAgent(truncate(userAgent, 512));
        token.setFamily(family);
        repository.save(token);

        return rawToken;
    }

    @Override
    @Transactional
    public RotateResult rotate(String rawToken, String userAgent) {
        String hash = hash(rawToken);

        RefreshToken token = repository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Token de rafraîchissement invalide."));

        if (token.isRevoked()) {
            // Token déjà révoqué présenté → vol potentiel : révoquer toute la famille
            repository.revokeAllByFamily(token.getFamily());
            log.warn("THEFT DETECTED — token révoqué réutilisé. Famille {} révoquée. user={}",
                    token.getFamily(), token.getUser().getUsername());
            throw new RefreshTokenTheftException(
                    "Activité suspecte détectée. Toutes vos sessions ont été fermées.");
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException("Token de rafraîchissement expiré.");
        }
        if (token.getUserAgent() != null && !token.getUserAgent().equals(truncate(userAgent, 512))) {
            log.warn("User-Agent mismatch on refresh — user={}", token.getUser().getUsername());
        }

        // Rotation : révoquer le token courant
        token.setRevoked(true);
        repository.save(token);

        // Émettre un nouveau token dans la même famille (expiration glissante)
        String newRaw = UUID.randomUUID().toString();
        RefreshToken next = new RefreshToken();
        next.setTokenHash(hash(newRaw));
        next.setUser(token.getUser());
        next.setExpiresAt(Instant.now().plus(expirationHours, ChronoUnit.HOURS)); // glissante
        next.setCreatedAt(Instant.now());
        next.setRevoked(false);
        next.setUserAgent(truncate(userAgent, 512));
        next.setFamily(token.getFamily());
        repository.save(next);

        log.debug("Rotation OK — user={} famille={}", token.getUser().getUsername(), token.getFamily());
        return new RotateResult(newRaw, token.getUser());
    }

    @Override
    @Transactional
    public void revokeToken(String rawToken) {
        repository.findByTokenHash(hash(rawToken)).ifPresent(t -> {
            t.setRevoked(true);
            repository.save(t);
        });
    }

    @Override
    @Transactional
    public void revokeAllForUser(User user) {
        repository.revokeAllActiveByUser(user);
        log.info("Toutes les sessions révoquées pour user={}", user.getUsername());
    }

    // ── Utilitaires ──────────────────────────────────────────────────────────

    private String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 non disponible", e);
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
