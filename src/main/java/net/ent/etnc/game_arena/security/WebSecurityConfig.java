package net.ent.etnc.game_arena.security;

import net.ent.etnc.game_arena.security.jwt.AuthJwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration de Spring Security.
 * <p>
 * Points clés :
 * - Stateless : pas de session HTTP (chaque requête est authentifiée via le token JWT)
 * - CSRF désactivé : inutile pour une API REST consommée par un client externe
 * - Seule la route /api/v1/auth/** est accessible sans authentification
 * - BCrypt est utilisé pour hacher les mots de passe
 */
@Configuration
@EnableMethodSecurity   // Active @PreAuthorize sur les méthodes des controllers et services
public class WebSecurityConfig {

    private final UserDetailsService userDetailsService;

    @Autowired
    public WebSecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /**
     * Bean PasswordEncoder : BCrypt est l'algorithme recommandé pour les mots de passe.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Expose l'AuthenticationManager comme bean Spring.
     * Utilisé par AuthController pour valider les credentials au login.
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity httpSecurity) {
        AuthenticationManagerBuilder builder =
                httpSecurity.getSharedObject(AuthenticationManagerBuilder.class);
        builder.userDetailsService(this.userDetailsService)
                .passwordEncoder(passwordEncoder());
        return builder.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://172.16.64.192:5173"
        ));

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of("*"));

        configuration.setExposedHeaders(List.of("Authorization"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * Configuration de la chaîne de filtres de sécurité.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthJwtFilter authJwtFilter) {
        // Désactivation CSRF et CORS (API REST, pas de form HTML)
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable);

        // Stateless : pas de session HTTP côté serveur
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Règles d'autorisation
        http.authorizeHttpRequests(auth -> auth
                // Login public (pas besoin de token pour se connecter)
                .requestMatchers(
                        "/api/v1/users/",
                        "/api/v1/users/login/",
                        "/api/v1/users/refresh/"
                ).permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS).permitAll()
                // Tout le reste nécessite une authentification JWT valide
                .anyRequest().authenticated()
        );

        // 401 pour token absent/invalide, 403 uniquement pour rôle insuffisant
        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(
                        (request, response, e) -> response.sendError(401, "Non authentifié"))
        );

        // Ajoute le filtre JWT AVANT le filtre d'authentification par username/password
        http.addFilterBefore(authJwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
