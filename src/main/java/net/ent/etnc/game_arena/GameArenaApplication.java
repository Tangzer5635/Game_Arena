package net.ent.etnc.game_arena;

import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.models.enumerations.Role;
import net.ent.etnc.game_arena.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class GameArenaApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameArenaApplication.class, args);
    }

    @Bean
    CommandLineRunner initUsers(UserRepository repo, PasswordEncoder encoder) {
        return args -> {
            if (repo.findByUsername("admin").isEmpty()) {
                User u = new User();
                u.setUsername("admin");
                u.setPassword(encoder.encode("admin"));
                u.setRole(Role.ADMIN);
                repo.save(u);
            }
        };
    }

}
