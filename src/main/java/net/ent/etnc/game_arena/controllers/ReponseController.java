package net.ent.etnc.game_arena.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.game_arena.commons.JsonPatchUtils;
import net.ent.etnc.game_arena.commons.RsqlFilterUtils;
import net.ent.etnc.game_arena.dtos.ReponseRequestDto;
import net.ent.etnc.game_arena.dtos.ReponseResponseDto;
import net.ent.etnc.game_arena.dtos.assemblers.ReponseAssembler;
import net.ent.etnc.game_arena.models.entities.Reponse;
import net.ent.etnc.game_arena.services.ReponseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/reponses")
public class ReponseController {

    private static final Map<String, String> FILTERABLE = Map.of("text", "text");

    private static final Set<String> PATCHABLE = Set.of("text", "estBonne");

    private final ReponseService reponseService;
    private final ReponseAssembler reponseAssembler;
    private final RsqlFilterUtils rsqlFilterUtils;
    private final JsonPatchUtils jsonPatchUtils;

    @Autowired
    public ReponseController(ReponseService reponseService, ReponseAssembler reponseAssembler, RsqlFilterUtils rsqlFilterUtils, JsonPatchUtils jsonPatchUtils) {
        this.reponseService = reponseService;
        this.reponseAssembler = reponseAssembler;
        this.rsqlFilterUtils = rsqlFilterUtils;
        this.jsonPatchUtils = jsonPatchUtils;
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<ReponseResponseDto>> getAll(@RequestParam(required = false) String filter, @PageableDefault(size = 20) Pageable pageable) {
        Specification<Reponse> spec = rsqlFilterUtils.build(filter, FILTERABLE);

        return ResponseEntity.ok(reponseService.findAll(spec, pageable).map(reponseAssembler::toDto));
    }

    @GetMapping("/{id}/")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReponseResponseDto> getById(@PathVariable Long id) {
        return reponseService.findById(id)
                .map(reponse -> ResponseEntity.ok(reponseAssembler.toDto(reponse)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReponseResponseDto> post(@Valid @RequestBody ReponseRequestDto dto) {
        Reponse reponse = reponseAssembler.toEntity(dto);

        return ResponseEntity.ok(reponseAssembler.toDto(reponseService.create(reponse)));
    }

    @PutMapping("/{id}/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReponseResponseDto> put(@PathVariable Long id, @Valid @RequestBody ReponseRequestDto dto) {
        if (!reponseService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        dto.setId(id);
        Reponse reponse = reponseAssembler.toEntity(dto);

        return ResponseEntity.ok(reponseAssembler.toDto(reponseService.update(reponse)));
    }

    @PatchMapping(value = "/{id}/", consumes = "application/json-patch+json")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReponseResponseDto> patch(@PathVariable Long id, @RequestBody String patch) {
        Optional<Reponse> existing = reponseService.findById(id);

        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ReponseResponseDto patched = jsonPatchUtils.apply(patch, reponseAssembler.toDto(existing.get()), ReponseResponseDto.class, PATCHABLE);

        Reponse entity = reponseAssembler.toEntity(
                ReponseRequestDto.builder()
                        .id(id)
                        .text(patched.getText())
                        .estBonne(patched.isEstBonne())
                        .build()
        );
        return ResponseEntity.ok(reponseAssembler.toDto(reponseService.update(entity)
                ));
    }

    @DeleteMapping("/{id}/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!reponseService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        reponseService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}