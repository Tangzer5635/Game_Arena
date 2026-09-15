package net.ent.etnc.game_arena.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.game_arena.models.enumerations.EtatSalon;
import net.ent.etnc.game_arena.repositories.GameStateRepository;
import net.ent.etnc.game_arena.repositories.SalonRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Nettoie périodiquement les salons et parties abandonnés.
 * <p>
 * Deux règles TTL :
 * <ul>
 *   <li><b>Salons OUVERT</b> sans activité depuis {@code SALON_TTL_MINUTES} minutes →
 *       supprimés (hôte parti sans fermer proprement, réseau coupé…)</li>
 *   <li><b>Parties EN_COURS</b> ({@link net.ent.etnc.game_arena.models.entities.GameState})
 *       sans activité depuis {@code GAME_TTL_MINUTES} minutes →
 *       données de jeu supprimées (bug, crash du scheduler interne…)</li>
 * </ul>
 * Les joueurs encore connectés reçoivent une notification WebSocket {@code /deleted}
 * pour être redirigés vers le dashboard.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalonTtlScheduler {

    /** Durée d'inactivité avant suppression d'un salon en attente. */
    private static final int SALON_TTL_MINUTES = 30;

    /** Durée d'inactivité avant nettoyage d'une partie bloquée. */
    private static final int GAME_TTL_MINUTES  = 45;

    private final SalonRepository       salonRepository;
    private final GameStateRepository   gameStateRepository;
    private final SimpMessagingTemplate messaging;

    /**
     * Supprime les salons OUVERT inactifs depuis plus de {@value SALON_TTL_MINUTES} minutes.
     * Tourne toutes les 5 minutes.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1_000L)
    @Transactional
    public void cleanExpiredSalons() {
        Instant cutoff = Instant.now().minus(SALON_TTL_MINUTES, ChronoUnit.MINUTES);

        var expired = salonRepository.findByEtatAndLastActivityAtBefore(EtatSalon.OUVERT, cutoff);

        if (expired.isEmpty()) return;

        expired.forEach(salon -> {
            String code = salon.getCode();
            salonRepository.delete(salon);
            messaging.convertAndSend("/topic/salon/" + code + "/deleted", "");
            log.info("Salon {} supprimé par TTL ({} min d'inactivité)", code, SALON_TTL_MINUTES);
        });
    }

    /**
     * Supprime les états de partie bloqués (crash, bug de scheduling).
     * Tourne toutes les 10 minutes.
     */
    @Scheduled(fixedDelay = 10 * 60 * 1_000L)
    @Transactional
    public void cleanStaleGameStates() {
        Instant cutoff = Instant.now().minus(GAME_TTL_MINUTES, ChronoUnit.MINUTES);

        var stale = gameStateRepository.findByLastActivityAtBefore(cutoff);
        if (stale.isEmpty()) return;

        stale.forEach(state -> {
            gameStateRepository.delete(state);
            log.warn("GameState du salon {} supprimé par TTL ({} min sans activité)",
                    state.getSalonCode(), GAME_TTL_MINUTES);
        });
    }
}
