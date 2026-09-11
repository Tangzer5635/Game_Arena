package net.ent.etnc.game_arena.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.game_arena.commons.JsonPatchUtils;
import net.ent.etnc.game_arena.commons.RsqlFilterUtils;
import net.ent.etnc.game_arena.dtos.QuestionRequestDto;
import net.ent.etnc.game_arena.dtos.QuestionResponseDto;
import net.ent.etnc.game_arena.dtos.assemblers.QuestionAssembler;
import net.ent.etnc.game_arena.models.entities.Question;
import net.ent.etnc.game_arena.services.QuestionService;
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
@RequestMapping("/api/v1/questions")
public class QuestionController {

    private static final Map<String, String> FILTERABLE = Map.of("text", "text");

    private static final Set<String> PATCHABLE = Set.of("text");

    private final QuestionService questionService;
    private final QuestionAssembler questionAssembler;
    private final RsqlFilterUtils rsqlFilterUtils;
    private final JsonPatchUtils jsonPatchUtils;

    @Autowired
    public QuestionController(QuestionService questionService, QuestionAssembler questionAssembler, RsqlFilterUtils rsqlFilterUtils, JsonPatchUtils jsonPatchUtils) {
        this.questionService = questionService;
        this.questionAssembler = questionAssembler;
        this.rsqlFilterUtils = rsqlFilterUtils;
        this.jsonPatchUtils = jsonPatchUtils;
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<QuestionResponseDto>> getAll(@RequestParam(required = false) String filter, @PageableDefault(size = 20) Pageable pageable) {
        Specification<Question> spec = rsqlFilterUtils.build(filter, FILTERABLE);
        return ResponseEntity.ok(questionService.findAll(spec, pageable).map(questionAssembler::toDto));
    }

    @GetMapping("/{id}/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<QuestionResponseDto> getById(@PathVariable Long id) {
        return questionService.findById(id)
                .map(question -> ResponseEntity.ok(questionAssembler.toDto(question)
                )).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuestionResponseDto> post(@Valid @RequestBody QuestionRequestDto dto) {
        Question question = questionAssembler.toEntity(dto);
        Question createdQuestion = questionService.create(question);

        if (dto.getIdResponses() != null && !dto.getIdResponses().isEmpty()) {
            createdQuestion = questionService.addReponses(createdQuestion.getId(), dto.getIdResponses());
        }
        return ResponseEntity.ok(questionAssembler.toDto(createdQuestion));
    }

    @PutMapping("/{id}/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuestionResponseDto> put(
            @PathVariable Long id,
            @Valid @RequestBody QuestionRequestDto dto
    ) {
        if (!questionService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        dto.setId(id);
        Question question = questionAssembler.toEntity(dto);
        return ResponseEntity.ok(questionAssembler.toDto(questionService.update(question))
        );
    }

    @PatchMapping(value = "/{id}/", consumes = "application/json-patch+json")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuestionResponseDto> patch(@PathVariable Long id, @RequestBody String patch) {
        Optional<Question> existing = questionService.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        QuestionResponseDto patched = jsonPatchUtils.apply(patch,questionAssembler.toDto(existing.get()), QuestionResponseDto.class, PATCHABLE);
        Question entity = questionAssembler.toEntity(
                QuestionRequestDto.builder()
                        .id(id)
                        .text(patched.getText())
                        .build()
        );

        return ResponseEntity.ok(questionAssembler.toDto(questionService.update(entity))
        );
    }

    @DeleteMapping("/{id}/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!questionService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        questionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reponses")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<QuestionResponseDto> addReponses(@PathVariable Long id, @RequestBody List<Long> reponseIds) {
        return ResponseEntity.ok(questionAssembler.toDto(questionService.addReponses(id, reponseIds))
        );
    }

    @DeleteMapping("/{id}/reponses/{reponseId}/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<QuestionResponseDto> removeReponse(@PathVariable Long id, @PathVariable Long reponseId) {
        return ResponseEntity.ok(questionAssembler.toDto(questionService.removeReponse(id, reponseId))
        );
    }
}