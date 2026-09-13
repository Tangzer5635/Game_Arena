package net.ent.etnc.game_arena.repositories;

import net.ent.etnc.game_arena.models.entities.Reponse;
import net.ent.etnc.game_arena.repositories.commons.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReponseRepository extends BaseRepository<Reponse> {

    Optional<Reponse> findByText(String text);

}