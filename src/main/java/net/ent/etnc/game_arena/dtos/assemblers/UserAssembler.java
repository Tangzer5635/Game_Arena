package net.ent.etnc.game_arena.dtos.assemblers;

import net.ent.etnc.game_arena.dtos.UserRequestDto;
import net.ent.etnc.game_arena.dtos.UserResponseDto;
import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.models.enumerations.Role;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class UserAssembler {

    public UserResponseDto toDto(User user) {

        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    public List<UserResponseDto> toDtos(Collection<User> users) {

        return users.stream().map(this::toDto).toList();
    }

    public User toEntity(UserRequestDto dto) {

        User user = new User();

        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setRole(Role.USER);

        return user;
    }
}