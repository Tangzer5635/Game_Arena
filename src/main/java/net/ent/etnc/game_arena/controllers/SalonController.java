package net.ent.etnc.game_arena.controllers;

import net.ent.etnc.game_arena.dtos.SalonResponseDto;
import net.ent.etnc.game_arena.dtos.assemblers.SalonAssembler;
import net.ent.etnc.game_arena.models.entities.Salon;
import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.models.enumerations.EtatSalon;
import net.ent.etnc.game_arena.services.SalonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SalonResponseDto> create(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Salon salon = salonService.create(user.getId());
        return ResponseEntity.ok(salonAssembler.toDto(salon));
    }

    @GetMapping("/{code}/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SalonResponseDto> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(salonAssembler.toDto(salonService.findByCode(code)));
    }

    @PostMapping("/{code}/join")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SalonResponseDto> join(@PathVariable String code, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(salonAssembler.toDto(salonService.addUser(code, user.getId())));
    }

    @PostMapping("/{code}/start")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SalonResponseDto> start(@PathVariable String code, @RequestParam Long quizId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Salon salon = salonService.start(code, user.getId(), quizId);
        return ResponseEntity.ok(salonAssembler.toDto(salon));
    }

    @DeleteMapping("/{code}/users/{userId}/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SalonResponseDto> removeUser(@PathVariable String code, @PathVariable Long userId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (!user.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Salon salon = salonService.removeUser(code, userId);
        if (salon == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(salonAssembler.toDto(salon));
    }

    @PutMapping("/{code}/etat/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SalonResponseDto> changeEtat(@PathVariable String code, @RequestParam EtatSalon etat) {
        return ResponseEntity.ok(salonAssembler.toDto(salonService.changeEtat(code, etat)));
    }

    @DeleteMapping("/{code}/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> delete(@PathVariable String code, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        salonService.delete(code, user.getId());
        return ResponseEntity.noContent().build();
    }
}