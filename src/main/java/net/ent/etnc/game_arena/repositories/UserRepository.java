package net.ent.etnc.game_arena.repositories;

import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.repositories.commons.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends BaseRepository<User> {
    /** Utilisé par UserDetailsServiceImpl pour charger un User depuis son username. */
    Optional<User> findByUsername(String username);

}