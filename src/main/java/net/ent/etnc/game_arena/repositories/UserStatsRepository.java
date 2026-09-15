package net.ent.etnc.game_arena.repositories;

import net.ent.etnc.game_arena.models.entities.UserStats;
import net.ent.etnc.game_arena.repositories.commons.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserStatsRepository extends BaseRepository<UserStats> {

    Optional<UserStats> findByUserId(Long userId);
}
