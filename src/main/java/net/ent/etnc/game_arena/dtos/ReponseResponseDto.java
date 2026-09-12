package net.ent.etnc.game_arena.dtos;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReponseResponseDto {

    private Long id;

    private String text;

    private boolean estBonne;
}