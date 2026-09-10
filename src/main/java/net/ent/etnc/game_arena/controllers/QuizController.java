package net.ent.etnc.game_arena.controllers;

import net.ent.etnc.game_arena.models.entities.Quiz;
import net.ent.etnc.game_arena.dtos.QuizDto;
import net.ent.etnc.game_arena.dtos.assemblers.QuizAssembler;
import net.ent.etnc.game_arena.services.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/quizs")
public class QuizController {

    private final QuizService quizService;
    private final QuizAssembler quizAssembler;

    @Autowired
    public QuizController(QuizService quizService, QuizAssembler quizAssembler) {
        this.quizService = quizService;
        this.quizAssembler = quizAssembler;
    }

}