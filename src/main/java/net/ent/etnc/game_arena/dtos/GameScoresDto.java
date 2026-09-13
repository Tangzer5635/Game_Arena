package net.ent.etnc.game_arena.dtos;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameScoresDto {

    private Map<String, Integer> scores;
    private int answeredCount;
    private int totalPlayers;
}
