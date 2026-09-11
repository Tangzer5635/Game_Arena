package net.ent.etnc.game_arena.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.game_arena.commons.JsonPatchUtils;
import net.ent.etnc.game_arena.commons.RsqlFilterUtils;
import net.ent.etnc.game_arena.dtos.QuizRequestDto;
import net.ent.etnc.game_arena.dtos.QuizResponseDto;
import net.ent.etnc.game_arena.dtos.assemblers.QuizAssembler;
import net.ent.etnc.game_arena.models.entities.Quiz;
import net.ent.etnc.game_arena.services.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/quizs")
public class QuizController {

    private static final Map<String, String> FILTERABLE = Map.of("titre", "titre", "description", "description");

    private static final Set<String> PATCHABLE = Set.of("titre", "description");

    private final QuizService quizService;
    private final QuizAssembler quizAssembler;
    private final RsqlFilterUtils rsqlFilterUtils;
    private final JsonPatchUtils jsonPatchUtils;

    @Autowired
    public QuizController(QuizService quizService, QuizAssembler quizAssembler, RsqlFilterUtils rsqlFilterUtils, JsonPatchUtils jsonPatchUtils) {
        this.quizService = quizService;
        this.quizAssembler = quizAssembler;
        this.rsqlFilterUtils = rsqlFilterUtils;
        this.jsonPatchUtils = jsonPatchUtils;
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<QuizResponseDto>> getAll(@RequestParam(required = false) String filter, @PageableDefault(size = 20) Pageable pageable) {
        Specification<Quiz> spec = rsqlFilterUtils.build(filter, FILTERABLE);

        return ResponseEntity.ok(quizService.findAll(spec, pageable).map(quizAssembler::toDto));
    }

    @GetMapping("/{id}/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<QuizResponseDto> getById(@PathVariable Long id) {
        return quizService.findById(id)
                .map(quiz -> ResponseEntity.ok(quizAssembler.toDto(quiz)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuizResponseDto> post(@Valid @RequestBody QuizRequestDto dto) {
        Quiz quiz = quizAssembler.toEntity(dto);
        Quiz createdQuiz = quizService.create(quiz);
        if (dto.getIdQuestions() != null && !dto.getIdQuestions().isEmpty()) {
            createdQuiz = quizService.addQuestions(createdQuiz.getId(), dto.getIdQuestions());
        }
        return ResponseEntity.ok(quizAssembler.toDto(createdQuiz));
    }

    @PutMapping("/{id}/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuizResponseDto> put(@PathVariable Long id, @Valid @RequestBody QuizRequestDto dto) {
        if (!quizService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        dto.setId(id);
        Quiz quiz = quizAssembler.toEntity(dto);
        return ResponseEntity.ok(quizAssembler.toDto(quizService.update(quiz)));
    }

    @PatchMapping(value = "/{id}/", consumes = "application/json-patch+json")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuizResponseDto> patch(@PathVariable Long id, @RequestBody String patch) {
        Optional<Quiz> existing = quizService.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        QuizResponseDto patched = jsonPatchUtils.apply(patch, quizAssembler.toDto(existing.get()), QuizResponseDto.class, PATCHABLE);
        Quiz entity = quizAssembler.toEntity(
                QuizRequestDto.builder()
                        .id(id)
                        .titre(patched.getTitre())
                        .description(patched.getDescription())
                        .build()
        );

        return ResponseEntity.ok(quizAssembler.toDto(quizService.update(entity)));
    }

    @DeleteMapping("/{id}/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!quizService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        quizService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/questions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<QuizResponseDto> addQuestions(@PathVariable Long id, @RequestBody List<Long> questionIds) {
        return ResponseEntity.ok(quizAssembler.toDto(quizService.addQuestions(id, questionIds)));
    }

    @DeleteMapping("/{id}/questions/{questionId}/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<QuizResponseDto> removeQuestion(@PathVariable Long id, @PathVariable Long questionId) {
        return ResponseEntity.ok(quizAssembler.toDto(quizService.removeQuestion(id, questionId))
        );
    }
}