package net.ent.etnc.game_arena.dtos.assemblers;

import net.ent.etnc.game_arena.dtos.UserDto;
import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.models.enumerations.Role;
import net.ent.etnc.game_arena.services.UserService;
import net.ent.etnc.game_arena.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserAssembler {

    public UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .build();
    }

    public List<UserDto> toDtos(List<User> users) {
        return users.stream()
                .map(this::toDto)
                .toList();
    }

    public User toEntity(UserDto userDto) throws ServiceException {
        User user = new User();

        user.setId(userDto.getId());
        user.setUsername(userDto.getUsername());
        user.setPassword(userDto.getPassword());

        // Rôle par défaut
        user.setRole(Role.USER);

        return user;
    }
}