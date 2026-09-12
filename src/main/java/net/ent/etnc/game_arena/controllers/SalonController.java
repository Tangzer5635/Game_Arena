package net.ent.etnc.game_arena.controllers;

import net.ent.etnc.game_arena.dtos.SalonResponseDto;
import net.ent.etnc.game_arena.dtos.assemblers.SalonAssembler;
import net.ent.etnc.game_arena.models.entities.Salon;
import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.models.enumerations.EtatSalon;
import net.ent.etnc.game_arena.services.SalonService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @DeleteMapping("/{code}/users/{userId}/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SalonResponseDto> removeUser(@PathVariable String code, @PathVariable Long userId) {
        return ResponseEntity.ok(salonAssembler.toDto(salonService.removeUser(code, userId)));
    }

    @PutMapping("/{code}/etat/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SalonResponseDto> changeEtat(@PathVariable String code, @RequestParam EtatSalon etat) {
        return ResponseEntity.ok(salonAssembler.toDto(salonService.changeEtat(code, etat)));
    }

    @DeleteMapping("/{code}/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        salonService.delete(code);
        return ResponseEntity.noContent().build();
    }
}