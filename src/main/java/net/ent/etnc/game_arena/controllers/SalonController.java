package net.ent.etnc.game_arena.controllers;

import net.ent.etnc.game_arena.dtos.SalonResponseDto;
import net.ent.etnc.game_arena.dtos.assemblers.SalonAssembler;
import net.ent.etnc.game_arena.models.entities.SalonEntity;
import net.ent.etnc.game_arena.models.entities.User;
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

    /** Crée un salon. L'utilisateur authentifié en devient l'hôte. */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SalonResponseDto> create(
            @RequestParam(defaultValue = "8") int maxPlayers,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        SalonEntity salon = salonService.create(user.getId(), maxPlayers);
        return ResponseEntity.ok(salonAssembler.toDto(salon));
    }

    /** Retourne les infos d'un salon par son code (pour afficher le lobby). */
    @GetMapping("/{code}/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SalonResponseDto> get(@PathVariable String code) {
        return ResponseEntity.ok(salonAssembler.toDto(salonService.findByCode(code)));
    }

    /** Fait rejoindre l'utilisateur authentifié dans le salon identifié par {@code code}. */
    @PostMapping("/{code}/users/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SalonResponseDto> join(
            @PathVariable String code,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        SalonEntity salon = salonService.addUser(code, user.getId());
        return ResponseEntity.ok(salonAssembler.toDto(salon));
    }

    /** Fait quitter l'utilisateur authentifié du salon. Si hôte → salon supprimé. */
    @DeleteMapping("/{code}/users/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> leave(
            @PathVariable String code,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        salonService.removeUser(code, user.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Lance la partie dans un salon en choisissant le quiz.
     * Seul l'hôte peut appeler cet endpoint.
     */
    @PostMapping("/{code}/start/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SalonResponseDto> start(
            @PathVariable String code,
            @RequestParam Long quizId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        SalonEntity salon = salonService.start(code, user.getId(), quizId);
        return ResponseEntity.ok(salonAssembler.toDto(salon));
    }

    /** Supprime le salon (hôte uniquement). */
    @DeleteMapping("/{code}/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> delete(
            @PathVariable String code,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        salonService.delete(code, user.getId());
        return ResponseEntity.noContent().build();
    }
}
