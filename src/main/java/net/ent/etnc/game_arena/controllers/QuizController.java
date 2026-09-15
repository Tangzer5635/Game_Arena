package net.ent.etnc.game_arena.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.game_arena.commons.JsonPatchUtils;
import net.ent.etnc.game_arena.commons.RsqlFilterUtils;
import net.ent.etnc.game_arena.dtos.CreateQuizFullDto;
import net.ent.etnc.game_arena.dtos.QuizRequestDto;
import net.ent.etnc.game_arena.dtos.QuizResponseDto;
import net.ent.etnc.game_arena.dtos.assemblers.QuizAssembler;
import net.ent.etnc.game_arena.models.entities.Question;
import net.ent.etnc.game_arena.models.entities.Quiz;
import net.ent.etnc.game_arena.models.entities.Reponse;
import net.ent.etnc.game_arena.models.enumerations.TypeQuestion;
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
    public QuizController(QuizService quizService, QuizAssembler quizAssembler,
                          RsqlFilterUtils rsqlFilterUtils, JsonPatchUtils jsonPatchUtils) {
        this.quizService = quizService;
        this.quizAssembler = quizAssembler;
        this.rsqlFilterUtils = rsqlFilterUtils;
        this.jsonPatchUtils = jsonPatchUtils;
    }

    // ── Lecture ──────────────────────────────────────────────────────────────

    @GetMapping("/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<QuizResponseDto>> getAll(
            @RequestParam(required = false) String filter,
            @PageableDefault(size = 20) Pageable pageable
    ) {
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

    // ── Création ─────────────────────────────────────────────────────────────

    /**
     * Crée un quiz complet (titre + questions + réponses) en une requête.
     * Accessible à tout USER.
     */
    @PostMapping("/full")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<QuizResponseDto> createFull(@RequestBody CreateQuizFullDto dto) {
        Quiz quiz = new Quiz();
        quiz.setTitre(dto.getTitre());
        quiz.setDescription(dto.getDescription());
        appendQuestions(quiz, dto.getQuestions());
        return ResponseEntity.ok(quizAssembler.toDto(quizService.create(quiz)));
    }

    // ── Modification titre/description ────────────────────────────────────────

    @PutMapping("/{id}/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<QuizResponseDto> put(
            @PathVariable Long id,
            @Valid @RequestBody QuizRequestDto dto
    ) {
        Quiz quiz = quizService.findById(id).orElse(null);
        if (quiz == null) return ResponseEntity.notFound().build();

        // Met à jour les champs sur l'entité managée (version conservée)
        quiz.setTitre(dto.getTitre());
        quiz.setDescription(dto.getDescription());

        return ResponseEntity.ok(quizAssembler.toDto(quizService.update(quiz)));
    }

    @PatchMapping(value = "/{id}/", consumes = "application/json-patch+json")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<QuizResponseDto> patch(@PathVariable Long id, @RequestBody String patch) {
        Quiz quiz = quizService.findById(id).orElse(null);
        if (quiz == null) return ResponseEntity.notFound().build();

        QuizResponseDto patched = jsonPatchUtils.apply(
                patch, quizAssembler.toDto(quiz), QuizResponseDto.class, PATCHABLE
        );

        quiz.setTitre(patched.getTitre());
        quiz.setDescription(patched.getDescription());

        return ResponseEntity.ok(quizAssembler.toDto(quizService.update(quiz)));
    }

    // ── Suppression ───────────────────────────────────────────────────────────

    /**
     * Supprime le quiz ET toutes ses questions/réponses (cascade ALL + orphanRemoval).
     * Ouvert aux USER (pas seulement ADMIN) pour que le créateur puisse supprimer son quiz.
     */
    @DeleteMapping("/{id}/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!quizService.existsById(id)) return ResponseEntity.notFound().build();
        quizService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Gestion des questions dans un quiz ───────────────────────────────────

    /**
     * Ajoute une question COMPLÈTE (texte + réponses) à un quiz existant.
     * C'est l'endpoint utilisé par QuizDetails pour ajouter une question.
     */
    @PostMapping("/{id}/questions/full")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<QuizResponseDto> addFullQuestion(
            @PathVariable Long id,
            @RequestBody CreateQuizFullDto.CreateQuestionDto questionDto
    ) {
        Quiz quiz = quizService.findById(id)
                .orElse(null);
        if (quiz == null) return ResponseEntity.notFound().build();

        Question question = buildQuestion(questionDto);
        quiz.addQuestion(question);
        return ResponseEntity.ok(quizAssembler.toDto(quizService.update(quiz)));
    }

    /**
     * Remplace une question existante (texte + type + réponses).
     */
    @PutMapping("/{id}/questions/{questionId}/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<QuizResponseDto> updateQuestion(
            @PathVariable Long id,
            @PathVariable Long questionId,
            @RequestBody CreateQuizFullDto.CreateQuestionDto questionDto
    ) {
        Quiz quiz = quizService.findById(id).orElse(null);
        if (quiz == null) return ResponseEntity.notFound().build();

        // Retire l'ancienne question (orphanRemoval la supprimera avec ses réponses)
        quiz.getQuestions().stream()
                .filter(q -> q.getId().equals(questionId))
                .findFirst()
                .ifPresent(quiz::removeQuestion);

        // Ajoute la version mise à jour
        quiz.addQuestion(buildQuestion(questionDto));
        return ResponseEntity.ok(quizAssembler.toDto(quizService.update(quiz)));
    }

    /** Retire une question d'un quiz (et la supprime via orphanRemoval). */
    @DeleteMapping("/{id}/questions/{questionId}/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<QuizResponseDto> removeQuestion(
            @PathVariable Long id,
            @PathVariable Long questionId
    ) {
        return ResponseEntity.ok(quizAssembler.toDto(quizService.removeQuestion(id, questionId)));
    }

    /** Associe des questions existantes (par IDs) à un quiz. */
    @PostMapping("/{id}/questions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<QuizResponseDto> addQuestions(
            @PathVariable Long id,
            @RequestBody List<Long> questionIds
    ) {
        return ResponseEntity.ok(quizAssembler.toDto(quizService.addQuestions(id, questionIds)));
    }

    // ── Helpers privés ────────────────────────────────────────────────────────

    private void appendQuestions(Quiz quiz, List<CreateQuizFullDto.CreateQuestionDto> dtos) {
        for (CreateQuizFullDto.CreateQuestionDto qDto : dtos) {
            quiz.addQuestion(buildQuestion(qDto));
        }
    }

    private Question buildQuestion(CreateQuizFullDto.CreateQuestionDto qDto) {
        Question question = new Question();
        question.setText(qDto.getText());
        question.setType(qDto.getType() == null ? TypeQuestion.CHOIX : qDto.getType());
        if (qDto.getReponses() != null) {
            for (CreateQuizFullDto.CreateReponseDto rDto : qDto.getReponses()) {
                Reponse reponse = new Reponse();
                reponse.setText(rDto.getText());
                reponse.setEstBonne(rDto.isEstBonne());
                question.addReponse(reponse);
            }
        }
        return question;
    }
}
