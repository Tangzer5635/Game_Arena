package net.ent.etnc.game_arena.dtos.assemblers;

import net.ent.etnc.game_arena.services.UserService;
import net.ent.etnc.game_arena.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserAssembler {

    private final UserService userService;

    @Autowired
    public UserAssembler(UserService userService) {
        this.userService = userService;
    }

}