package net.ent.etnc.game_arena.services.game;

import lombok.RequiredArgsConstructor;
import net.ent.etnc.game_arena.dtos.*;
import net.ent.etnc.game_arena.models.entities.GameState;
import net.ent.etnc.game_arena.models.entities.Question;
import net.ent.etnc.game_arena.models.entities.Reponse;
import net.ent.etnc.game_arena.models.entities.SalonEntity;
import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.models.enumerations.TypeQuestion;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Centralise tous les envois WebSocket liés à une partie.
 * <p>
 * Chaque méthode correspond à un événement du jeu et broadcast
 * sur le topic approprié. Séparer les broadcasts du service métier
 * permet de tester {@code GameServiceImpl} sans infrastructure WebSocket.
 */
@Component
@RequiredArgsConstructor
public class GameBroadcaster {

    private final SimpMessagingTemplate messaging;

    // ── Topics ────────────────────────────────────────────────────────────────

    private String topicGame(String code, String event) {
        return "/topic/game/" + code + "/" + event;
    }

    // ── Broadcasts ────────────────────────────────────────────────────────────

    /**
     * Diffuse le tick du compte à rebours (3, 2, 1) à tous les joueurs.
     *
     * @param code  code du salon
     * @param value valeur courante (3, 2 ou 1)
     */
    public void sendCountdown(String code, int value) {
        messaging.convertAndSend(
                topicGame(code, "countdown"),
                GameCountdownDto.builder().value(value).build()
        );
    }

    /**
     * Diffuse la question courante à tous les joueurs.
     * Ne contient pas la bonne réponse — l'information reste côté serveur.
     *
     * @param code     code du salon
     * @param question entité question à diffuser
     * @param index    index 0-based de la question dans le quiz
     * @param total    nombre total de questions du quiz
     * @param startTs  timestamp serveur du début (epoch ms) pour synchroniser le timer front
     */
    public void sendQuestion(String code, Question question, int index, int total, long startTs) {
        List<GameReponseDto> repDtos = question.getType() == TypeQuestion.SAISIE_LIBRE
                ? List.of()
                : question.getReponses().stream()
                        .map(r -> GameReponseDto.builder().id(r.getId()).text(r.getText()).build())
                        .toList();

        messaging.convertAndSend(
                topicGame(code, "question"),
                GameQuestionDto.builder()
                        .questionId(question.getId())
                        .text(question.getText())
                        .reponses(repDtos)
                        .questionNumber(index + 1)
                        .totalQuestions(total)
                        .saisieLibre(question.getType() == TypeQuestion.SAISIE_LIBRE)
                        .serverTimestamp(startTs)
                        .build()
        );
    }

    /**
     * Envoie le résultat individuel d'une réponse au joueur concerné uniquement.
     * Topic : {@code /topic/game/{code}/answer-result/{userId}}
     *
     * @param code   code du salon
     * @param result DTO du résultat à envoyer
     */
    public void sendAnswerResult(String code, GameAnswerResultDto result) {
        messaging.convertAndSend(
                topicGame(code, "answer-result/" + result.getUserId()),
                result
        );
    }

    /**
     * Diffuse les scores et streaks actuels à tous les joueurs.
     * Appelé après chaque réponse pour mettre à jour le classement live.
     *
     * @param code   code du salon
     * @param salon  salon courant (pour la liste des joueurs)
     * @param state  état de la partie (scores, streaks, répondants)
     */
    public void sendScores(String code, SalonEntity salon, GameState state) {
        messaging.convertAndSend(
                topicGame(code, "scores"),
                buildScoresDto(salon, state)
        );
    }

    /**
     * Diffuse la révélation de la bonne réponse à tous les joueurs.
     * Déclenche la phase "reveal" côté front (coloration des boutons).
     *
     * @param code  code du salon
     * @param bonne bonne réponse à révéler (peut être null si données corrompues)
     * @param salon salon pour la liste des joueurs
     * @param state état pour les scores finaux de ce round
     */
    public void sendReveal(String code, Reponse bonne, SalonEntity salon, GameState state) {
        messaging.convertAndSend(
                topicGame(code, "reveal"),
                GameRevealDto.builder()
                        .correctReponseId(bonne != null ? bonne.getId() : null)
                        .correctAnswer(bonne != null ? bonne.getText() : null)
                        .scores(buildNamedScores(salon, state))
                        .build()
        );
    }

    /**
     * Diffuse les résultats finaux à tous les joueurs et déclenche l'affichage du podium.
     *
     * @param code  code du salon
     * @param salon salon pour la liste des joueurs
     * @param state état final (scores définitifs)
     */
    public void sendResults(String code, SalonEntity salon, GameState state) {
        Set<String> allUsers = new HashSet<>();
        Map<String, Integer> streakMap = new LinkedHashMap<>();
        for (User u : salon.getUsers()) {
            allUsers.add(u.getUsername());
            streakMap.put(u.getUsername(), state.getStreaks().getOrDefault(u.getId(), 0));
        }

        messaging.convertAndSend(
                topicGame(code, "results"),
                GameScoresDto.builder()
                        .scores(buildNamedScores(salon, state))
                        .streaks(streakMap)
                        .answeredUsers(allUsers)
                        .answeredCount(salon.getUsers().size())
                        .totalPlayers(salon.getUsers().size())
                        .build()
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private GameScoresDto buildScoresDto(SalonEntity salon, GameState state) {
        Set<String> answered = new HashSet<>();
        Map<String, Integer> streakMap = new LinkedHashMap<>();

        for (User u : salon.getUsers()) {
            if (state.getAnsweredUserIds().contains(u.getId())) answered.add(u.getUsername());
            streakMap.put(u.getUsername(), state.getStreaks().getOrDefault(u.getId(), 0));
        }

        return GameScoresDto.builder()
                .scores(buildNamedScores(salon, state))
                .streaks(streakMap)
                .answeredUsers(answered)
                .answeredCount(state.getAnsweredUserIds().size())
                .totalPlayers(salon.getUsers().size())
                .build();
    }

    private Map<String, Integer> buildNamedScores(SalonEntity salon, GameState state) {
        Map<String, Integer> named = new LinkedHashMap<>();
        for (User u : salon.getUsers()) {
            named.put(u.getUsername(), state.getScores().getOrDefault(u.getId(), 0));
        }
        return named;
    }
}
