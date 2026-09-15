package net.ent.etnc.game_arena.dtos;

import lombok.*;

import java.util.List;
import net.ent.etnc.game_arena.models.enumerations.TypeQuestion;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponseDto {
    private Long id;
    private String text;
    private TypeQuestion type;
    private List<ReponseResponseDto> reponses;
}