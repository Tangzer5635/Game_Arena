package net.ent.etnc.game_arena.dtos;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizRequestDto {

    private Long id;
    private String titre;
    private String description;
    private List<Long> idQuestions;
}