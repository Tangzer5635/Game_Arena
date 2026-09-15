package net.ent.etnc.game_arena.dtos;

import lombok.*;
import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class GameRevealDto {
    private Long correctReponseId;
    private String correctAnswer;
    private Map<String, Integer> scores;
}
