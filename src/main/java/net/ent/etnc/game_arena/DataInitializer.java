package net.ent.etnc.game_arena;

import net.ent.etnc.game_arena.models.entities.Question;
import net.ent.etnc.game_arena.models.entities.Quiz;
import net.ent.etnc.game_arena.models.entities.Reponse;
import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.models.enumerations.Role;
import net.ent.etnc.game_arena.repositories.QuizRepository;
import net.ent.etnc.game_arena.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seed de démonstration.
 * ddl-auto: create → la base est vide à chaque démarrage.
 * On crée tout directement, pas de findOrCreate.
 * cascade PERSIST sur Quiz→Question→Reponse : un seul save(quiz) suffit.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            QuizRepository quizRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.quizRepository = quizRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {

        // ── Users ──

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin"));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        User tanguy = new User();
        tanguy.setUsername("tanguy");
        tanguy.setPassword(passwordEncoder.encode("1595"));
        tanguy.setRole(Role.USER);
        userRepository.save(tanguy);

        User gloo = new User();
        gloo.setUsername("gloo");
        gloo.setPassword(passwordEncoder.encode("1234"));
        gloo.setRole(Role.USER);
        userRepository.save(gloo);

        // ── Quiz "Culture générale" ──

        Quiz quiz = new Quiz();
        quiz.setTitre("Culture générale");
        quiz.setDescription("Quiz de démonstration Game Arena");

        // Q1
        Question q1 = new Question();
        q1.setText("Quelle est la capitale de la France ?");
        q1.addReponse(reponse("Paris", true));
        q1.addReponse(reponse("Rennes", false));
        q1.addReponse(reponse("Lyon", false));
        q1.addReponse(reponse("Marseille", false));
        quiz.addQuestion(q1);

        // Q2
        Question q2 = new Question();
        q2.setText("Combien de continents y a-t-il ?");
        q2.addReponse(reponse("5", false));
        q2.addReponse(reponse("6", false));
        q2.addReponse(reponse("7", true));
        q2.addReponse(reponse("8", false));
        quiz.addQuestion(q2);

        // Q3
        Question q3 = new Question();
        q3.setText("En quelle année l'homme a-t-il marché sur la Lune ?");
        q3.addReponse(reponse("1965", false));
        q3.addReponse(reponse("1969", true));
        q3.addReponse(reponse("1972", false));
        q3.addReponse(reponse("1959", false));
        quiz.addQuestion(q3);

        quizRepository.save(quiz);
    }

    private Reponse reponse(String text, boolean estBonne) {
        Reponse r = new Reponse();
        r.setText(text);
        r.setEstBonne(estBonne);
        return r;
    }
}
