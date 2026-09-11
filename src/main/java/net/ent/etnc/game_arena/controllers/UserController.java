package net.ent.etnc.game_arena.controllers;

import jakarta.servlet.http.HttpServletRequest;
import net.ent.etnc.game_arena.dtos.UserRequestDto;
import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.security.jwt.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import net.ent.etnc.game_arena.services.RefreshTokenService;

import java.time.Duration;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.security.refresh-token.expiration-hours:2}")
    private int refreshExpirationHours;

    @Autowired
    public UserController(AuthenticationManager authenticationManager,
                          JwtUtils jwtUtils,
                          RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * POST /api/v1/auth/login/
     * Body   : {"username": "admin", "password": "admin"}
     * Retour : header Authorization: Bearer {jwt}  +  cookie jp_refresh (httpOnly)
     */
    @PostMapping("login/")
    public ResponseEntity<Void> login(@RequestBody UserRequestDto authDto, HttpServletRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authDto.getUsername(), authDto.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = (User) authentication.getPrincipal();
            String jwt = jwtUtils.generateJwtToken(authentication);
            String rawRefresh = refreshTokenService.createToken(
                    user, request.getHeader(HttpHeaders.USER_AGENT)
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(rawRefresh).toString())
                    .build();

        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * POST /api/v1/auth/refresh/
     * Cookie  : jp_refresh (httpOnly, envoyé automatiquement par le navigateur)
     * Retour  : header Authorization: Bearer {new_jwt}  +  nouveau cookie jp_refresh (rotation)
     * <p>
     * En cas de vol détecté (token révoqué réutilisé) → 401 + toute la famille révoquée.
     */
    @PostMapping("refresh/")
    public ResponseEntity<Void> refresh(
            @CookieValue(name = "jp_refresh", required = false) String rawToken,
            HttpServletRequest request) {

        if (rawToken == null) return ResponseEntity.status(401).build();

        try {
            RefreshTokenService.RotateResult result = refreshTokenService.rotate(
                    rawToken, request.getHeader(HttpHeaders.USER_AGENT)
            );
            String newJwt = jwtUtils.generateJwtTokenForUser(result.user());

            return ResponseEntity.ok()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + newJwt)
                    .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.newRawToken()).toString())
                    .build();

        } catch (Exception e) {
            // Token invalide, expiré ou vol détecté → effacer le cookie
            return ResponseEntity.status(401)
                    .header(HttpHeaders.SET_COOKIE, buildClearCookie().toString())
                    .build();
        }
    }

    /**
     * POST /api/v1/users/logout/
     * Cookie : jp_refresh — révoque le token en base et efface le cookie.
     */
    @PostMapping("logout/")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "jp_refresh", required = false) String rawToken) {

        if (rawToken != null) {
            try {
                refreshTokenService.revokeToken(rawToken);
            } catch (Exception ignored) {
            }
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildClearCookie().toString())
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ResponseCookie buildRefreshCookie(String rawToken) {
        return ResponseCookie.from("jp_refresh", rawToken)
                .httpOnly(true)
                .secure(false)          // passer à true en production (HTTPS)
                .sameSite("Strict")
                .path("/api/v1/users/refresh")   // cookie envoyé uniquement vers les endpoints d'users
                .maxAge(Duration.ofHours(refreshExpirationHours))
                .build();
    }

    private ResponseCookie buildClearCookie() {
        return ResponseCookie.from("jp_refresh", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/api/v1/users")
                .maxAge(0)
                .build();
    }

}