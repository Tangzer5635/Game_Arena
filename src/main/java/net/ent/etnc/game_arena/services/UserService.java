package net.ent.etnc.game_arena.services;

import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.services.commons.Service;

public interface UserService extends Service<User, Long> {
    User register(User user);
}