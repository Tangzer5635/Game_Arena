package net.ent.etnc.game_arena.dtos;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameQuestionDto {

    private Long questionId;
    private String text;
    private List<GameReponseDto> reponses;
    private int questionNumber;
    private int totalQuestions;
}
