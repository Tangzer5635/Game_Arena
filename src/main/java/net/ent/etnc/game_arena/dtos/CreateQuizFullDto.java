package net.ent.etnc.game_arena.dtos;

import lombok.*;

import java.util.List;
import net.ent.etnc.game_arena.models.enumerations.TypeQuestion;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuizFullDto {

    private String titre;
    private String description;
    private List<CreateQuestionDto> questions;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateQuestionDto {
        private String text;
        private TypeQuestion type = TypeQuestion.CHOIX;
        private List<CreateReponseDto> reponses;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateReponseDto {
        private String text;
        private boolean estBonne;
    }
}
