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
    private Long createurId;
    private EtatSalon etat;
    private Long quizId;
    private List<UserResponseDto> users;
}