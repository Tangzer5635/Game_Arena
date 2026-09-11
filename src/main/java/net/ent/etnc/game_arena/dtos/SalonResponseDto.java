package net.ent.etnc.game_arena.dtos;

import lombok.*;
import net.ent.etnc.game_arena.models.enumerations.EtatSalon;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonResponseDto {
    private Long id;
    private String code;
    private EtatSalon etat;
    private List<UserResponseDto> users;
}