package net.ent.etnc.game_arena.dtos;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRequestDto {
    private Long id;
    private String text;
    private List<Long> idResponses;
}