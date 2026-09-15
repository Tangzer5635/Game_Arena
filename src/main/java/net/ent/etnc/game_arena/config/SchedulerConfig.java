package net.ent.etnc.game_arena.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configure le {@link TaskScheduler} Spring utilisé par {@code GameServiceImpl}
 * pour les timers de question et de reveal.
 * <p>
 * Avantages par rapport à un {@code ScheduledExecutorService} manuel :
 * <ul>
 *   <li>Cycle de vie géré par Spring (arrêt propre au shutdown)</li>
 *   <li>Exceptions loguées plutôt que silencieuses</li>
 *   <li>Pool de threads dimensionnable via configuration</li>
 * </ul>
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(8);
        scheduler.setThreadNamePrefix("game-scheduler-");
        scheduler.setErrorHandler(t -> {
            // log l'exception sans tuer le thread — important pour les timers de parties
            System.err.println("[game-scheduler] Exception non gérée : " + t.getMessage());
            t.printStackTrace();
        });
        scheduler.initialize();
        return scheduler;
    }
}
