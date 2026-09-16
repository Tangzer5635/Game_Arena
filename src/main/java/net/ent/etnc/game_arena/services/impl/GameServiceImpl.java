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
import net.ent.etnc.game_arena.services.QuizLoadingService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {

    private static final Duration QUESTION_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REVEAL_DELAY     = Duration.ofSeconds(4);
    private static final Duration COUNTDOWN_STEP   = Duration.ofSeconds(1);

    private final SalonService        salonService;
    private final QuizRepository      quizRepository;
    private final QuizLoadingService  quizLoadingService;
    private final GameStateRepository gameStateRepository;
    private final UserStatsRepository userStatsRepository;
    private final UserService         userService;
    private final ScoreCalculator     scoreCalculator;
    private final GameBroadcaster     broadcaster;
    private final TaskScheduler       taskScheduler;

    private final Map<String, ScheduledFuture<?>> timers       = new ConcurrentHashMap<>();
    private final Map<String, Boolean>            starting     = new ConcurrentHashMap<>();
    private final Map<String, Integer>            revealedIndex = new ConcurrentHashMap<>();

    // ── Démarrage ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void startGame(String code, Long userId) {
        // JOIN FETCH users pour éviter LazyInitializationException dans le scheduler
        SalonEntity salon = salonService.findByCodeWithUsers(code);

        if (!salon.getCreateurId().equals(userId)) {
            throw new ServiceException("Seul l'hôte peut démarrer la partie.");
        }
        if (starting.putIfAbsent(code, true) != null
                || gameStateRepository.findBySalonCode(code).isPresent()) {
            return;
        }

        try {
            Long quizId = salon.getQuizId();
            if (quizId == null) throw new ServiceException("Aucun quiz associé à ce salon.");

            Quiz quiz = loadQuiz(quizId);
            if (quiz.getQuestions().isEmpty()) throw new ServiceException("Le quiz ne contient aucune question.");
            quiz.getQuestions().forEach(q -> q.getReponses().size());

            GameState state = new GameState();
            state.setSalonCode(code);
            state.setQuizId(quizId);
            state.initForPlayers(salon.getUsers().stream().map(User::getId).toList());
            gameStateRepository.save(state);

            scheduleCountdown(code, quizId);

        } catch (RuntimeException e) {
            gameStateRepository.findBySalonCode(code).ifPresent(gameStateRepository::delete);
            throw e;
        } finally {
            starting.remove(code);
        }
    }

    // ── Réponse ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void answer(String code, Long userId, Long reponseId, String answer) {
        SalonEntity salon = salonService.findByCodeWithUsers(code);
        GameState   state = requireGameState(code);

        if (!state.getScores().containsKey(userId)) throw new ServiceException("Vous ne participez pas à cette partie.");
        if (!state.markAnswered(userId))             throw new ServiceException("Vous avez déjà répondu à cette question.");

        Quiz     quiz     = loadQuiz(state.getQuizId());
        Question question = quiz.getQuestions().get(state.getCurrentQuestionIndex());
        Reponse  bonne    = findBonneReponse(question);

        boolean correct   = evaluateAnswer(question, bonne, reponseId, answer);
        long    elapsedMs = Math.max(0L, System.currentTimeMillis() - state.getQuestionStartedAt());

        int points    = 0;
        int streak    = state.getStreaks().getOrDefault(userId, 0);
        int multiplier = 1;

        if (correct) {
            boolean bonusActive = state.getStreakBonuses().getOrDefault(userId, false);
            int base = scoreCalculator.basePoints(elapsedMs);
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
                .userId(userId).correct(correct).correctReponseId(bonne.getId())
                .pointsEarned(points).score(state.getScores().getOrDefault(userId, 0))
                .elapsedMs(elapsedMs).streak(streak).multiplier(multiplier)
                .build();

        broadcaster.sendAnswerResult(code, result);
        broadcaster.sendScores(code, salon, state);

        if (state.allAnswered()) {
            cancelTimer(code);
            doReveal(code, salon, state, quiz);
        }
    }

    // ── Reconnexion ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public void rejoin(String code, Long userId) {
        gameStateRepository.findBySalonCode(code).ifPresent(state -> {
            if (state.getCurrentQuestionIndex() < 0) return;
            SalonEntity salon = salonService.findByCodeWithUsers(code);
            Quiz quiz = loadQuiz(state.getQuizId());
            Question q = quiz.getQuestions().get(state.getCurrentQuestionIndex());
            broadcaster.sendQuestion(code, q, state.getCurrentQuestionIndex(),
                    quiz.getQuestions().size(), state.getQuestionStartedAt());
            broadcaster.sendScores(code, salon, state);
        });
    }

    // ── Countdown → Question → Timer → Reveal → Suivante ─────────────────────

    private void scheduleCountdown(String code, Long quizId) {

        for (int i = 3; i >= 1; i--) {
            final int val = i;

            taskScheduler.schedule(
                    () -> broadcaster.sendCountdown(code, val),
                    Instant.now().plus(COUNTDOWN_STEP.multipliedBy(3 - i))
            );
        }

        taskScheduler.schedule(
                () -> startQuestion(code, quizId, 0),
                Instant.now().plus(COUNTDOWN_STEP.multipliedBy(3))
        );
    }

    private void startQuestion(String code, Long quizId, int index) {

        Quiz quiz = loadQuiz(quizId);

        gameStateRepository.findBySalonCode(code).ifPresent(state -> {

            state.setCurrentQuestionIndex(index);
            state.clearAnswers();
            state.setQuestionStartedAt(System.currentTimeMillis());
            state.touch();

            gameStateRepository.save(state);

            SalonEntity salon = salonService.findByCodeWithUsers(code);

            Question question = quiz.getQuestions().get(index);

            broadcaster.sendQuestion(
                    code,
                    question,
                    index,
                    quiz.getQuestions().size(),
                    state.getQuestionStartedAt()
            );

            scheduleTimer(code, quizId);
        });
    }

    private void scheduleTimer(String code, Long quizId) {

        cancelTimer(code);

        timers.put(code, taskScheduler.schedule(
                () -> {

                    timers.remove(code);

                    gameStateRepository.findBySalonCode(code).ifPresent(state -> {

                        Quiz quiz = loadQuiz(quizId);

                        SalonEntity salon =
                                salonService.findByCodeWithUsers(code);

                        doReveal(code, salon, state, quiz);
                    });

                },
                Instant.now().plus(QUESTION_TIMEOUT)
        ));
    }

    private void cancelTimer(String code) {
        ScheduledFuture<?> f = timers.remove(code);
        if (f != null) f.cancel(false);
    }

    @Transactional
    protected void doReveal(String code, SalonEntity salon, GameState state, Quiz quiz) {
        int idx = state.getCurrentQuestionIndex();
        if (revealedIndex.putIfAbsent(code, idx) != null) return;

        for (User u : salon.getUsers()) {
            if (!state.getAnsweredUserIds().contains(u.getId())) {
                state.getStreaks().put(u.getId(), 0);
                state.getStreakBonuses().put(u.getId(), false);
            }
        }
        gameStateRepository.save(state);

        Question q     = quiz.getQuestions().get(idx);
        Reponse  bonne = q.getReponses().stream().filter(Reponse::isEstBonne).findFirst().orElse(null);
        broadcaster.sendReveal(code, bonne, salon, state);

        taskScheduler.schedule(
                () -> advanceOrFinish(code, quiz.getId()),
                Instant.now().plus(REVEAL_DELAY)
        );
    }

    @Transactional
    protected void advanceOrFinish(String code, Long quizId) {

        GameState state = gameStateRepository
                .findBySalonCode(code)
                .orElse(null);

        if (state == null) return;

        Quiz quiz = loadQuiz(quizId);

        revealedIndex.remove(code);

        int next = state.getCurrentQuestionIndex() + 1;

        if (next >= quiz.getQuestions().size()) {

            SalonEntity salon = salonService.findByCodeWithUsers(code);

            broadcaster.sendResults(code, salon, state);

            updatePlayerStats(salon, state, quiz);

            gameStateRepository.delete(state);

            salonService.cleanup(code);

            log.info("Partie du salon {} terminée", code);

        } else {

            startQuestion(code, quizId, next);
        }
    }

    // ── Stats joueurs ─────────────────────────────────────────────────────────

    @Transactional
    protected void updatePlayerStats(SalonEntity salon, GameState state, Quiz quiz) {
        int totalQuestions = quiz.getQuestions().size();
        for (User u : salon.getUsers()) {
            Long uid      = u.getId();
            int  score    = state.getScores().getOrDefault(uid, 0);
            int  streak   = state.getStreaks().getOrDefault(uid, 0);
            int  bonnes   = score > 0 ? Math.min(totalQuestions, (int) Math.ceil((double) score / 500)) : 0;
            int  mauvaises = totalQuestions - bonnes;

            UserStats stats = userStatsRepository.findByUserId(uid)
                    .orElseGet(() -> { UserStats s = new UserStats(); s.setUser(u); return s; });
            stats.enregistrerPartie(score, bonnes, mauvaises, streak);
            userStatsRepository.save(stats);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private GameState requireGameState(String code) {
        return gameStateRepository.findBySalonCode(code)
                .orElseThrow(() -> new ServiceException("La partie n'a pas encore commencé."));
    }

    protected Quiz loadQuiz(Long quizId) {
        return quizLoadingService.loadQuiz(quizId);
    }

    private Reponse findBonneReponse(Question question) {
        return question.getReponses().stream().filter(Reponse::isEstBonne).findFirst()
                .orElseThrow(() -> new ServiceException("Pas de bonne réponse définie."));
    }

    private boolean evaluateAnswer(Question question, Reponse bonne, Long reponseId, String answer) {
        if (question.getType() == TypeQuestion.SAISIE_LIBRE)
            return scoreCalculator.matchesFreeText(answer, bonne.getText());
        return bonne.getId().equals(reponseId);
    }
}
