package net.ent.etnc.game_arena.repositories;

import net.ent.etnc.game_arena.models.entities.Question;
import net.ent.etnc.game_arena.repositories.commons.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends BaseRepository<Question> {

    Optional<Question> findByText(String text);

    @Query("""
    SELECT DISTINCT q
    FROM Question q
    LEFT JOIN FETCH q.reponses
    WHERE q.id IN :ids
""")
    List<Question> findAllByIdWithReponses(@Param("ids") List<Long> ids);

}