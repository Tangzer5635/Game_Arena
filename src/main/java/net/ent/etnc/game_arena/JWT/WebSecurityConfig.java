package net.ent.etnc.game_arena.JWT;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration Spring Security pour l'API REST Jurassic Park.
 *
 * <p>Principes appliqués :
 * <ul>
 *   <li><b>Stateless</b> — aucune HttpSession créée, chaque requête est autonome ;</li>
 *   <li><b>CSRF désactivé</b> — sans cookie de session, pas de vecteur d'attaque CSRF ;</li>
 *   <li><b>JWT</b> — le filtre {@link AuthJwtFilter} valide le token à chaque requête ;</li>
 *   <li><b>401 vs 403</b> — 401 = non authentifié (token absent/expiré),
 *       403 = authentifié mais rôle insuffisant.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

    private final AuthJwtFilter authJwtFilter;

    @Autowired
    public WebSecurityConfig(AuthJwtFilter authJwtFilter) {
        this.authJwtFilter = authJwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // ① Stateless — jamais de HttpSession
                .sessionManagement(s -> s
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // ② CSRF off — safe car pas de cookie de session
                .csrf(AbstractHttpConfigurer::disable)
                // ③ Routes publiques vs protégées
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()
                        .anyRequest().authenticated())
                // ④ 401 pour les non-authentifiés, 403 pour les droits insuffisants
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> res.sendError(401, "Non authentifié"))
                        .accessDeniedHandler((req, res, e) -> res.sendError(403, "Accès refusé")))
                // ⑤ Filtre JWT avant le filtre login standard de Spring
                .addFilterBefore(authJwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Encodeur BCrypt pour les mots de passe.
     * Injecté dans l'Init pour hasher les mots de passe au démarrage,
     * et utilisé par AuthenticationManager pour vérifier lors du login.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Expose l'AuthenticationManager Spring Security comme bean.
     * Injecté dans AuthController pour vérifier les identifiants au login.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
