package net.ent.etnc.game_arena.controllers;

import net.ent.etnc.game_arena.models.entities.Salon;
import net.ent.etnc.game_arena.dtos.SalonDto;
import net.ent.etnc.game_arena.dtos.assemblers.SalonAssembler;
import net.ent.etnc.game_arena.services.SalonService;
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
@RequestMapping("/api/v1/salons")
public class SalonController {

    private final SalonService salonService;
    private final SalonAssembler salonAssembler;

    @Autowired
    public SalonController(SalonService salonService, SalonAssembler salonAssembler) {
        this.salonService = salonService;
        this.salonAssembler = salonAssembler;
    }

}