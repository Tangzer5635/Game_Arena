package net.ent.etnc.game_arena.dtos;

import lombok.*;
import java.util.Map;
import java.util.Set;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class GameScoresDto {
    private Map<String, Integer> scores;
    private Map<String, Integer> streaks;
    /** usernames qui ont déjà répondu (pour l'indicateur visuel) */
    private Set<String> answeredUsers;
    private int answeredCount;
    private int totalPlayers;
}
