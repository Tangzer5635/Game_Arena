package net.ent.etnc.game_arena.dtos.assemblers;

import lombok.RequiredArgsConstructor;
import net.ent.etnc.game_arena.dtos.QuizRequestDto;
import net.ent.etnc.game_arena.dtos.QuizResponseDto;
import net.ent.etnc.game_arena.models.entities.Quiz;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class QuizAssembler {

    private final QuestionAssembler questionAssembler;

    public QuizResponseDto toDto(Quiz quiz) {

        return QuizResponseDto.builder()
                .id(quiz.getId())
                .titre(quiz.getTitre())
                .description(quiz.getDescription())
                .questions(questionAssembler.toDtos(quiz.getQuestions()))
                .build();
    }

    public List<QuizResponseDto> toDtos(Collection<Quiz> quizzes) {

        return quizzes.stream().map(this::toDto).toList();
    }

    public Quiz toEntity(QuizRequestDto dto) {
        Quiz quiz = new Quiz();
        quiz.setId(dto.getId());
        quiz.setTitre(dto.getTitre());
        quiz.setDescription(dto.getDescription());
        return quiz;
    }
}