package net.ent.etnc.game_arena.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameAnswerRequestDto {

    private Long userId;
    private Long reponseId;
}
