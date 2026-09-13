package net.ent.etnc.game_arena.repositories;

import net.ent.etnc.game_arena.models.entities.Question;
import net.ent.etnc.game_arena.repositories.commons.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionRepository extends BaseRepository<Question> {

    Optional<Question> findByText(String text);

}