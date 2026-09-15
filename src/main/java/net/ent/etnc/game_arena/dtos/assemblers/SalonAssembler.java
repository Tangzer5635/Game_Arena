package net.ent.etnc.game_arena.dtos.assemblers;

import lombok.RequiredArgsConstructor;
import net.ent.etnc.game_arena.dtos.SalonResponseDto;
import net.ent.etnc.game_arena.models.entities.SalonEntity;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/** Convertit un {@link SalonEntity} en DTO exposable via l'API REST ou WebSocket. */
@Component
@RequiredArgsConstructor
public class SalonAssembler {

    private final UserAssembler userAssembler;

    public SalonResponseDto toDto(SalonEntity salon) {
        return SalonResponseDto.builder()
                .id(salon.getId())
                .code(salon.getCode())
                .createurId(salon.getCreateurId())
                .etat(salon.getEtat())
                .quizId(salon.getQuizId())
                .maxPlayers(salon.getMaxPlayers())
                .users(userAssembler.toDtos(salon.getUsers()))
                .build();
    }

    public List<SalonResponseDto> toDtos(Collection<SalonEntity> salons) {
        return salons.stream().map(this::toDto).toList();
    }
}
