package net.ent.etnc.game_arena.controllers;

import net.ent.etnc.game_arena.models.entities.Question;
import net.ent.etnc.game_arena.dtos.QuestionDto;
import net.ent.etnc.game_arena.dtos.assemblers.QuestionAssembler;
import net.ent.etnc.game_arena.services.QuestionService;
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
@RequestMapping("/api/v1/questions")
public class QuestionController {

    private final QuestionService questionService;
    private final QuestionAssembler questionAssembler;

    @Autowired
    public QuestionController(QuestionService questionService, QuestionAssembler questionAssembler) {
        this.questionService = questionService;
        this.questionAssembler = questionAssembler;
    }

}