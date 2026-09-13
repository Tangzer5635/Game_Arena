package net.ent.etnc.game_arena.services.impl;

import net.ent.etnc.game_arena.dtos.*;
import net.ent.etnc.game_arena.models.entities.Question;
import net.ent.etnc.game_arena.models.entities.Quiz;
import net.ent.etnc.game_arena.models.entities.Reponse;
import net.ent.etnc.game_arena.models.entities.Salon;
import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.models.enumerations.EtatSalon;
import net.ent.etnc.game_arena.repositories.QuizRepository;
import net.ent.etnc.game_arena.services.GameService;
import net.ent.etnc.game_arena.services.SalonService;
import net.ent.etnc.game_arena.services.commons.ServiceException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameServiceImpl implements GameService {

    private final SalonService salonService;
    private final QuizRepository quizRepository;
    private final SimpMessagingTemplate messaging;

    /**
     * Cache des quiz chargés (quiz complet avec questions + réponses).
     * Clé = code du salon, pour ne pas recharger à chaque réponse.
     */
    private final Map<String, Quiz> loadedQuizzes = new ConcurrentHashMap<>();

    public GameServiceImpl(
            SalonService salonService,
            QuizRepository quizRepository,
            SimpMessagingTemplate messaging
    ) {
        this.salonService = salonService;
        this.quizRepository = quizRepository;
        this.messaging = messaging;
    }

    @Override
    @Transactional(readOnly = true)
    public void startGame(String code) {

        Salon salon = salonService.findByCode(code);

        if (salon.getQuizId() == null) {
            throw new ServiceException("Aucun quiz n'est associé à ce salon.");
        }

        // JOIN FETCH charge les questions ; on force le chargement des réponses
        // dans la même transaction avant de mettre en cache
        Quiz quiz = quizRepository.findByIdWithQuestionsAndReponses(salon.getQuizId())
                .orElseThrow(() -> new ServiceException("Quiz introuvable."));

        if (quiz.getQuestions().isEmpty()) {
            throw new ServiceException("Le quiz ne contient aucune question.");
        }

        // Force l'init lazy des réponses (on est encore dans la transaction)
        for (Question q : quiz.getQuestions()) {
            q.getReponses().size();
        }

        // Met en cache pour les appels suivants (answer, nextQuestion)
        loadedQuizzes.put(code, quiz);

        // Init le jeu
        salon.initScores();
        salon.setCurrentQuestionIndex(0);
        salon.clearAnswers();

        // Broadcast Q1
        broadcastQuestion(code, salon, quiz);
    }

    @Override
    public GameAnswerResultDto answer(String code, Long userId, Long reponseId) {

        Salon salon = salonService.findByCode(code);
        Quiz quiz = loadedQuizzes.get(code);

        if (quiz == null || salon.getCurrentQuestionIndex() < 0) {
            throw new ServiceException("La partie n'a pas encore commencé.");
        }

        if (salon.getAnsweredUserIds().contains(userId)) {
            throw new ServiceException("Vous avez déjà répondu à cette question.");
        }

        Question question = quiz.getQuestions().get(salon.getCurrentQuestionIndex());

        // Trouver la bonne réponse
        Reponse bonneReponse = question.getReponses().stream()
                .filter(Reponse::isEstBonne)
                .findFirst()
                .orElseThrow(() -> new ServiceException("Pas de bonne réponse définie."));

        boolean correct = bonneReponse.getId().equals(reponseId);

        if (correct) {
            salon.addScore(userId, 1);
        }

        salon.getAnsweredUserIds().add(userId);

        // 1. Résultat individuel → topic dédié au joueur uniquement
        GameAnswerResultDto result = GameAnswerResultDto.builder()
                .userId(userId)
                .correct(correct)
                .correctReponseId(bonneReponse.getId())
                .score(salon.getScores().getOrDefault(userId, 0))
                .build();

        messaging.convertAndSend(
                "/topic/game/" + code + "/answer-result/" + userId,
                result
        );

        // 2. Scores mis à jour
        broadcastScores(code, salon);

        // 3. Si tout le monde a répondu → question suivante (broadcast en dernier)
        if (salon.allAnswered()) {
            nextQuestion(code);
        }

        return result;
    }

    @Override
    public void nextQuestion(String code) {

        Salon salon = salonService.findByCode(code);
        Quiz quiz = loadedQuizzes.get(code);

        if (quiz == null) {
            return;
        }

        int nextIndex = salon.getCurrentQuestionIndex() + 1;

        if (nextIndex >= quiz.getQuestions().size()) {
            // Fin de la partie
            salon.setEtat(EtatSalon.TERMINE);
            broadcastResults(code, salon);
            loadedQuizzes.remove(code);
            return;
        }

        salon.setCurrentQuestionIndex(nextIndex);
        salon.clearAnswers();
        broadcastQuestion(code, salon, quiz);
    }

    // ── Broadcasts privés ──

    private void broadcastQuestion(String code, Salon salon, Quiz quiz) {

        Question question = quiz.getQuestions().get(salon.getCurrentQuestionIndex());

        List<GameReponseDto> reponseDtos = question.getReponses().stream()
                .map(r -> GameReponseDto.builder()
                        .id(r.getId())
                        .text(r.getText())
                        .build())
                .toList();

        GameQuestionDto dto = GameQuestionDto.builder()
                .questionId(question.getId())
                .text(question.getText())
                .reponses(reponseDtos)
                .questionNumber(salon.getCurrentQuestionIndex() + 1)
                .totalQuestions(quiz.getQuestions().size())
                .build();

        messaging.convertAndSend("/topic/game/" + code + "/question", dto);
    }

    private void broadcastScores(String code, Salon salon) {

        // Convertit userId → username pour l'affichage
        Map<String, Integer> namedScores = new LinkedHashMap<>();
        for (User u : salon.getUsers()) {
            namedScores.put(u.getUsername(), salon.getScores().getOrDefault(u.getId(), 0));
        }

        GameScoresDto dto = GameScoresDto.builder()
                .scores(namedScores)
                .answeredCount(salon.getAnsweredUserIds().size())
                .totalPlayers(salon.getUsers().size())
                .build();

        messaging.convertAndSend("/topic/game/" + code + "/scores", dto);
    }

    private void broadcastResults(String code, Salon salon) {

        Map<String, Integer> namedScores = new LinkedHashMap<>();
        for (User u : salon.getUsers()) {
            namedScores.put(u.getUsername(), salon.getScores().getOrDefault(u.getId(), 0));
        }

        GameScoresDto dto = GameScoresDto.builder()
                .scores(namedScores)
                .answeredCount(salon.getUsers().size())
                .totalPlayers(salon.getUsers().size())
                .build();

        messaging.convertAndSend("/topic/game/" + code + "/results", dto);
    }
}
