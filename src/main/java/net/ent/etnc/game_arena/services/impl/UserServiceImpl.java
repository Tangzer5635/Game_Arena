package net.ent.etnc.game_arena.services.impl;

import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.repositories.UserRepository;
import net.ent.etnc.game_arena.services.UserService;
import net.ent.etnc.game_arena.services.commons.AbstractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends AbstractService<User, UserRepository> implements UserService {

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        super(userRepository);
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return super.create(user);
    }

    @Override
    public User findByUsername(String username) {
        return repository.findByUsername(username).orElse(null);
    }
}