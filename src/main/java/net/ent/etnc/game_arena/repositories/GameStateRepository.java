package net.ent.etnc.game_arena.repositories;

import net.ent.etnc.game_arena.models.entities.GameState;
import net.ent.etnc.game_arena.repositories.commons.BaseRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameStateRepository extends BaseRepository<GameState> {

    Optional<GameState> findBySalonCode(String salonCode);

    void deleteBySalonCode(String salonCode);

    /** Parties dont la dernière activité est antérieure au seuil (TTL). */
    List<GameState> findByLastActivityAtBefore(Instant cutoff);
}
