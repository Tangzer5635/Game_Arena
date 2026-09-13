package net.ent.etnc.game_arena.dtos.assemblers;

import lombok.RequiredArgsConstructor;
import net.ent.etnc.game_arena.dtos.SalonResponseDto;
import net.ent.etnc.game_arena.models.entities.Salon;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SalonAssembler {

    private final UserAssembler userAssembler;

    public SalonResponseDto toDto(Salon salon) {
        return SalonResponseDto.builder()
                .id(salon.getId())
                .code(salon.getCode())
                .createurId(salon.getCreateurId())
                .etat(salon.getEtat())
                .quizId(salon.getQuizId())
                .users(userAssembler.toDtos(salon.getUsers()))
                .build();
    }

    public List<SalonResponseDto> toDtos(Collection<Salon> salons) {
        return salons.stream()
                .map(this::toDto)
                .toList();
    }
}