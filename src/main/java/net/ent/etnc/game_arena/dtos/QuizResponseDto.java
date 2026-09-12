package net.ent.etnc.game_arena.dtos;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResponseDto {

    private Long id;
    private String titre;
    private String description;
    private List<QuestionResponseDto> questions;
}