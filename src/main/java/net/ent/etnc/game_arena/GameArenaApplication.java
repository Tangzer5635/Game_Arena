package net.ent.etnc.game_arena;

import net.ent.etnc.game_arena.models.entities.Question;
import net.ent.etnc.game_arena.models.entities.Quiz;
import net.ent.etnc.game_arena.models.entities.Reponse;
import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.models.enumerations.Role;
import net.ent.etnc.game_arena.repositories.QuestionRepository;
import net.ent.etnc.game_arena.repositories.QuizRepository;
import net.ent.etnc.game_arena.repositories.ReponseRepository;
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
    CommandLineRunner initData(
            UserRepository userRepository,
            QuizRepository quizRepository,
            QuestionRepository questionRepository,
            ReponseRepository reponseRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {

             /* USERS */

            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin"));
                admin.setRole(Role.ADMIN);
                userRepository.save(admin);
            }


            /* REPONSES QUESTION 1 */

            Reponse paris = new Reponse();
            paris.setText("Paris");
            paris.setEstBonne(true);

            Reponse rennes = new Reponse();
            rennes.setText("Rennes");
            rennes.setEstBonne(false);

            Reponse lyon = new Reponse();
            lyon.setText("Lyon");
            lyon.setEstBonne(false);

            Reponse marseille = new Reponse();
            marseille.setText("Marseille");
            marseille.setEstBonne(false);

            reponseRepository.save(paris);
            reponseRepository.save(rennes);
            reponseRepository.save(lyon);
            reponseRepository.save(marseille);


             /* QUESTION 1*/

            Question question1 = new Question();

            question1.setText("Quelle est la capitale de la France ?");

            question1.addReponse(paris);
            question1.addReponse(rennes);
            question1.addReponse(lyon);
            question1.addReponse(marseille);

            questionRepository.save(question1);

             /* QUIZ */

            Quiz quiz = new Quiz();
            quiz.setTitre("Culture générale");
            quiz.setDescription("Quiz de démonstration Game Arena");

             /* ASSOCIATION QUESTIONS -> QUIZ */

            quiz.addQuestion(question1);
            quizRepository.save(quiz);
        };
    }
}