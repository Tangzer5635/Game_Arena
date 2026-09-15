package net.ent.etnc.game_arena.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.game_arena.dtos.GameAnswerResultDto;
import net.ent.etnc.game_arena.models.entities.*;
import net.ent.etnc.game_arena.models.enumerations.EtatSalon;
import net.ent.etnc.game_arena.models.enumerations.TypeQuestion;
import net.ent.etnc.game_arena.repositories.GameStateRepository;
import net.ent.etnc.game_arena.repositories.QuizRepository;
import net.ent.etnc.game_arena.repositories.UserStatsRepository;
import net.ent.etnc.game_arena.services.GameService;
import net.ent.etnc.game_arena.services.SalonService;
import net.ent.etnc.game_arena.services.UserService;
import net.ent.etnc.game_arena.services.commons.ServiceException;
import net.ent.etnc.game_arena.services.game.GameBroadcaster;
import net.ent.etnc.game_arena.services.game.ScoreCalculator;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Orchestre le déroulement d'une partie de quiz multijoueur.
 * <p>
 * Délégation des responsabilités :
 * <ul>
 *   <li>{@link ScoreCalculator} — calcul des points et normalisation de la saisie libre</li>
 *   <li>{@link GameBroadcaster} — tous les envois WebSocket</li>
 *   <li>{@link GameStateRepository} — persistance de l'état live en base (survie aux redémarrages)</li>
 *   <li>{@link UserStatsRepository} — mise à jour du profil joueur en fin de partie</li>
 *   <li>{@link TaskScheduler} Spring — timers de question et de reveal (remplace le ScheduledExecutorService manuel)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {

    private static final Duration QUESTION_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REVEAL_DELAY     = Duration.ofSeconds(4);
    private static final Duration COUNTDOWN_STEP   = Duration.ofSeconds(1);

    private final SalonService         salonService;
    private final QuizRepository       quizRepository;
    private final GameStateRepository  gameStateRepository;
    private final UserStatsRepository  userStatsRepository;
    private final UserService          userService;
    private final ScoreCalculator      scoreCalculator;
    private final GameBroadcaster      broadcaster;
    private final TaskScheduler        taskScheduler;

    // Timers actifs — clé : code du salon
    private final Map<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();
    // Verrou de démarrage (évite le double-start concurrent)
    private final Map<String, Boolean> starting = new ConcurrentHashMap<>();
    // Index de la question déjà révélée (évite le double-reveal)
    private final Map<String, Integer> revealedIndex = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    // Démarrage de la partie
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lance le compte à rebours et démarre la première question.
     * <p>
     * Règles :
     * <ul>
     *   <li>Seul l'hôte peut démarrer</li>
     *   <li>Le salon doit avoir un quiz associé</li>
     *   <li>Le quiz doit contenir au moins une question</li>
     *   <li>Idempotent : un second appel concurrent est ignoré</li>
     * </ul>
     */
    @Override
    @Transactional
    public void startGame(String code, Long userId) {
        SalonEntity salon = salonService.findByCode(code);

        if (!salon.getCreateurId().equals(userId)) {
            throw new ServiceException("Seul l'hôte peut démarrer la partie.");
        }

        // Double-start guard
        if (starting.putIfAbsent(code, true) != null
                || gameStateRepository.findBySalonCode(code).isPresent()) {
            log.debug("Démarrage ignoré (salon {} déjà en cours)", code);
            return;
        }

        try {
            Long quizId = salon.getQuizId();
            if (quizId == null) throw new ServiceException("Aucun quiz associé à ce salon.");

            Quiz quiz = quizRepository.findByIdWithQuestionsAndReponses(quizId)
                    .orElseThrow(() -> new ServiceException("Quiz introuvable."));

            if (quiz.getQuestions().isEmpty()) throw new ServiceException("Le quiz ne contient aucune question.");

            // Force le chargement lazy des réponses dans la transaction courante
            quiz.getQuestions().forEach(q -> q.getReponses().size());

            // Crée et persiste l'état de la partie
            GameState state = new GameState();
            state.setSalonCode(code);
            state.setQuizId(quizId);
            state.initForPlayers(salon.getUsers().stream().map(User::getId).toList());
            gameStateRepository.save(state);

            // Compte à rebours 3-2-1, puis première question
            scheduleCountdown(code, quiz);

        } catch (RuntimeException e) {
            gameStateRepository.findBySalonCode(code).ifPresent(gameStateRepository::delete);
            throw e;
        } finally {
            starting.remove(code);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Réponse d'un joueur
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Traite la réponse d'un joueur à la question courante.
     * <p>
     * Règles :
     * <ul>
     *   <li>Le joueur doit être inscrit dans la partie (présent dans les scores)</li>
     *   <li>Une seule réponse par question (double-submit bloqué)</li>
     *   <li>Score = points vitesse × multiplicateur streak</li>
     *   <li>Streak ≥ 3 consécutifs → bonus ×2 activé pour la question suivante</li>
     *   <li>Si tous ont répondu → reveal immédiat (annule le timer)</li>
     * </ul>
     *
     * @param code      code du salon
     * @param userId    ID du joueur (tiré du Principal STOMP, non falsifiable)
     * @param reponseId ID de la réponse choisie (null pour saisie libre)
     * @param answer    texte saisi (null pour choix multiple)
     */
    @Override
    @Transactional
    public void answer(String code, Long userId, Long reponseId, String answer) {
        SalonEntity salon = salonService.findByCode(code);
        GameState state = requireGameState(code);

        if (!state.getScores().containsKey(userId)) {
            throw new ServiceException("Vous ne participez pas à cette partie.");
        }

        if (!state.markAnswered(userId)) {
            throw new ServiceException("Vous avez déjà répondu à cette question.");
        }

        Quiz quiz = loadQuiz(state.getQuizId());
        Question question = quiz.getQuestions().get(state.getCurrentQuestionIndex());
        Reponse bonne = findBonneReponse(question);

        boolean correct = evaluateAnswer(question, bonne, reponseId, answer);
        long elapsedMs = Math.max(0L, System.currentTimeMillis() - state.getQuestionStartedAt());

        // Mise à jour scores et streaks
        int points = 0;
        int streak = state.getStreaks().getOrDefault(userId, 0);
        int multiplier = 1;

        if (correct) {
            int base = scoreCalculator.basePoints(elapsedMs);
            boolean bonusActive = state.getStreakBonuses().getOrDefault(userId, false);
            points = scoreCalculator.applyMultiplier(base, bonusActive);
            if (bonusActive) { multiplier = 2; state.getStreakBonuses().put(userId, false); }

            streak++;
            if (scoreCalculator.triggersBonus(streak)) state.getStreakBonuses().put(userId, true);
            state.getStreaks().put(userId, streak);
            state.addScore(userId, points);
        } else {
            streak = 0;
            state.getStreaks().put(userId, 0);
            state.getStreakBonuses().put(userId, false);
        }

        gameStateRepository.save(state);

        GameAnswerResultDto result = GameAnswerResultDto.builder()
                .userId(userId).correct(correct)
                .correctReponseId(bonne.getId())
                .pointsEarned(points)
                .score(state.getScores().getOrDefault(userId, 0))
                .elapsedMs(elapsedMs).streak(streak).multiplier(multiplier)
                .build();

        broadcaster.sendAnswerResult(code, result);
        broadcaster.sendScores(code, salon, state);

        if (state.allAnswered()) {
            cancelTimer(code);
            doReveal(code, salon, state, quiz);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reconnexion mi-partie
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Renvoie l'état courant de la partie à un joueur qui vient de se reconnecter.
     * <p>
     * Si aucune partie n'est en cours dans ce salon, l'appel est silencieusement ignoré.
     *
     * @param code   code du salon
     * @param userId ID du joueur qui se reconnecte
     */
    @Override
    @Transactional(readOnly = true)
    public void rejoin(String code, Long userId) {
        gameStateRepository.findBySalonCode(code).ifPresent(state -> {
            if (state.getCurrentQuestionIndex() < 0) return;
            SalonEntity salon = salonService.findByCode(code);
            Quiz quiz = loadQuiz(state.getQuizId());
            Question q = quiz.getQuestions().get(state.getCurrentQuestionIndex());
            broadcaster.sendQuestion(code, q, state.getCurrentQuestionIndex(),
                    quiz.getQuestions().size(), state.getQuestionStartedAt());
            broadcaster.sendScores(code, salon, state);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internals — countdown, reveal, question suivante
    // ─────────────────────────────────────────────────────────────────────────

    private void scheduleCountdown(String code, Quiz quiz) {
        for (int i = 3; i >= 1; i--) {
            final int val = i;
            taskScheduler.schedule(
                    () -> broadcaster.sendCountdown(code, val),
                    Instant.now().plus(COUNTDOWN_STEP.multipliedBy(3 - i))
            );
        }
        taskScheduler.schedule(() -> startQuestion(code, quiz, 0),
                Instant.now().plus(COUNTDOWN_STEP.multipliedBy(3)));
    }

    private void startQuestion(String code, Quiz quiz, int index) {
        gameStateRepository.findBySalonCode(code).ifPresent(state -> {
            state.setCurrentQuestionIndex(index);
            state.clearAnswers();
            state.setQuestionStartedAt(System.currentTimeMillis());
            state.touch();
            gameStateRepository.save(state);

            SalonEntity salon = salonService.findByCode(code);
            broadcaster.sendQuestion(code, quiz.getQuestions().get(index),
                    index, quiz.getQuestions().size(), state.getQuestionStartedAt());

            scheduleTimer(code, quiz);
        });
    }

    private void scheduleTimer(String code, Quiz quiz) {
        cancelTimer(code);
        ScheduledFuture<?> f = taskScheduler.schedule(
                () -> {
                    timers.remove(code);
                    gameStateRepository.findBySalonCode(code).ifPresent(state -> {
                        SalonEntity salon = salonService.findByCode(code);
                        doReveal(code, salon, state, quiz);
                    });
                },
                Instant.now().plus(QUESTION_TIMEOUT)
        );
        timers.put(code, f);
    }

    private void cancelTimer(String code) {
        ScheduledFuture<?> f = timers.remove(code);
        if (f != null) f.cancel(false);
    }

    @Transactional
    protected void doReveal(String code, SalonEntity salon, GameState state, Quiz quiz) {
        int idx = state.getCurrentQuestionIndex();

        // Guard anti-double-reveal
        if (revealedIndex.putIfAbsent(code, idx) != null) return;

        // Remet les streaks à 0 pour les joueurs qui n'ont pas répondu
        for (User u : salon.getUsers()) {
            if (!state.getAnsweredUserIds().contains(u.getId())) {
                state.getStreaks().put(u.getId(), 0);
                state.getStreakBonuses().put(u.getId(), false);
            }
        }
        gameStateRepository.save(state);

        Question q = quiz.getQuestions().get(idx);
        Reponse bonne = q.getReponses().stream().filter(Reponse::isEstBonne).findFirst().orElse(null);
        broadcaster.sendReveal(code, bonne, salon, state);

        taskScheduler.schedule(() -> advanceOrFinish(code, salon, quiz),
                Instant.now().plus(REVEAL_DELAY));
    }

    @Transactional
    protected void advanceOrFinish(String code, SalonEntity salon, Quiz quiz) {
        GameState state = gameStateRepository.findBySalonCode(code).orElse(null);
        if (state == null) return;

        revealedIndex.remove(code);
        int next = state.getCurrentQuestionIndex() + 1;

        if (next >= quiz.getQuestions().size()) {
            // ── Fin de partie ──
            salon = salonService.findByCode(code); // reload
            broadcaster.sendResults(code, salon, state);
            updatePlayerStats(salon, state, quiz);
            gameStateRepository.delete(state);
            salonService.cleanup(code);
            log.info("Partie du salon {} terminée", code);
        } else {
            startQuestion(code, quiz, next);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mise à jour des stats joueurs (profil)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Met à jour les statistiques de chaque joueur à la fin de la partie.
     * Crée une ligne {@link UserStats} si le joueur n'en a pas encore.
     *
     * @param salon salon de la partie terminée
     * @param state état final (scores, streaks)
     * @param quiz  quiz joué (pour compter les questions)
     */
    @Transactional
    protected void updatePlayerStats(SalonEntity salon, GameState state, Quiz quiz) {
        int totalQuestions = quiz.getQuestions().size();

        for (User u : salon.getUsers()) {
            Long uid = u.getId();
            int score  = state.getScores().getOrDefault(uid, 0);
            int streak = state.getStreaks().getOrDefault(uid, 0);

            // Estimation des bonnes réponses à partir du score
            // (approximation : 1 bonne réponse ≈ score / MAX par question)
            int answered = state.getAnsweredUserIds().contains(uid)
                    ? (int) state.getAnsweredUserIds().stream().filter(id -> id.equals(uid)).count()
                    : 0;
            int bonnes    = score > 0 ? Math.min(totalQuestions, (int) Math.ceil((double) score / 500)) : 0;
            int mauvaises = totalQuestions - bonnes;

            UserStats stats = userStatsRepository.findByUserId(uid)
                    .orElseGet(() -> {
                        UserStats s = new UserStats();
                        s.setUser(u);
                        return s;
                    });

            stats.enregistrerPartie(score, bonnes, mauvaises, streak);
            userStatsRepository.save(stats);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private GameState requireGameState(String code) {
        return gameStateRepository.findBySalonCode(code)
                .orElseThrow(() -> new ServiceException("La partie n'a pas encore commencé."));
    }

    @Transactional(readOnly = true)
    protected Quiz loadQuiz(Long quizId) {
        return quizRepository.findByIdWithQuestionsAndReponses(quizId)
                .orElseThrow(() -> new ServiceException("Quiz introuvable."));
    }

    private Reponse findBonneReponse(Question question) {
        return question.getReponses().stream()
                .filter(Reponse::isEstBonne)
                .findFirst()
                .orElseThrow(() -> new ServiceException("Pas de bonne réponse définie."));
    }

    private boolean evaluateAnswer(Question question, Reponse bonne, Long reponseId, String answer) {
        if (question.getType() == TypeQuestion.SAISIE_LIBRE) {
            return scoreCalculator.matchesFreeText(answer, bonne.getText());
        }
        return bonne.getId().equals(reponseId);
    }
}
