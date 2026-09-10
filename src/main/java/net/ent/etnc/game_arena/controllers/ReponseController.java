package net.ent.etnc.game_arena.controllers;

import net.ent.etnc.game_arena.models.entities.Reponse;
import net.ent.etnc.game_arena.dtos.ReponseDto;
import net.ent.etnc.game_arena.dtos.assemblers.ReponseAssembler;
import net.ent.etnc.game_arena.services.ReponseService;
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
@RequestMapping("/api/v1/reponses")
public class ReponseController {

    private final ReponseService reponseService;
    private final ReponseAssembler reponseAssembler;

    @Autowired
    public ReponseController(ReponseService reponseService, ReponseAssembler reponseAssembler) {
        this.reponseService = reponseService;
        this.reponseAssembler = reponseAssembler;
    }

}