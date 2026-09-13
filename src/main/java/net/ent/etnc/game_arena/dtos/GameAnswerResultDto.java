package net.ent.etnc.game_arena.dtos;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameAnswerResultDto {

    private Long userId;
    private boolean correct;
    private Long correctReponseId;
    private int score;
}
