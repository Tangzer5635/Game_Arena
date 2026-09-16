package net.ent.etnc.game_arena.repositories;

import net.ent.etnc.game_arena.models.entities.Quiz;
import net.ent.etnc.game_arena.repositories.commons.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuizRepository extends BaseRepository<Quiz> {

    Optional<Quiz> findByTitre(String titre);

    @Query("""
    SELECT DISTINCT q
    FROM Quiz q
    LEFT JOIN FETCH q.questions
    WHERE q.id = :id
""")
    Optional<Quiz> findByIdWithQuestions(@Param("id") Long id);
}