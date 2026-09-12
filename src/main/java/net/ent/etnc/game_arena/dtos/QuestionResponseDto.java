package net.ent.etnc.game_arena.dtos;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponseDto {
    private Long id;
    private String text;
    private List<ReponseResponseDto> reponses;
}