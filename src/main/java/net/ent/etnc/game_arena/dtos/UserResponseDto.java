package net.ent.etnc.game_arena.dtos;

import lombok.*;
import net.ent.etnc.game_arena.models.enumerations.Role;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

    private Long id;

    private String username;

    private Role role;
}