package net.ent.etnc.game_arena.dtos.assemblers;

import lombok.RequiredArgsConstructor;
import net.ent.etnc.game_arena.dtos.QuestionRequestDto;
import net.ent.etnc.game_arena.dtos.QuestionResponseDto;
import net.ent.etnc.game_arena.models.entities.Question;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class QuestionAssembler {
    private final ReponseAssembler reponseAssembler;

    public QuestionResponseDto toDto(Question question) {
        return QuestionResponseDto.builder()
                .id(question.getId())
                .text(question.getText())
                .type(question.getType())
                .reponses(reponseAssembler.toDtos(question.getReponses()))
                .build();
    }

    public List<QuestionResponseDto> toDtos(Collection<Question> questions) {
        return questions.stream()
                .map(this::toDto)
                .toList();
    }

    public Question toEntity(QuestionRequestDto dto) {
        Question question = new Question();
        question.setId(dto.getId());
        question.setText(dto.getText());
        question.setType(dto.getType() == null ? net.ent.etnc.game_arena.models.enumerations.TypeQuestion.CHOIX : dto.getType());

        return question;
    }
}