package net.ent.etnc.game_arena.dtos;

import lombok.*;

import java.util.List;
import net.ent.etnc.game_arena.models.enumerations.TypeQuestion;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRequestDto {
    private Long id;
    private String text;
    private TypeQuestion type = TypeQuestion.CHOIX;
    private List<Long> idResponses;
}