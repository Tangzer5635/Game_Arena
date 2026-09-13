package net.ent.etnc.game_arena.services.impl;

import net.ent.etnc.game_arena.models.entities.Salon;
import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.models.entities.Quiz;
import net.ent.etnc.game_arena.models.enumerations.EtatSalon;
import net.ent.etnc.game_arena.services.SalonService;
import net.ent.etnc.game_arena.services.UserService;
import net.ent.etnc.game_arena.services.QuizService;
import net.ent.etnc.game_arena.services.commons.ServiceException;
import net.ent.etnc.game_arena.dtos.assemblers.SalonAssembler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SalonServiceImpl implements SalonService {

    private final Map<String, Salon> salons = new ConcurrentHashMap<>();

    private final UserService userService;
    private final QuizService quizService;
    private final SalonAssembler salonAssembler;
    private final SimpMessagingTemplate messagingTemplate;

    public SalonServiceImpl(
            UserService userService,
            QuizService quizService,
            SalonAssembler salonAssembler,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.userService = userService;
        this.quizService = quizService;
        this.salonAssembler = salonAssembler;
        this.messagingTemplate = messagingTemplate;
    }

    // Prévient les autres joueurs du salon (liste des joueurs, changement d'état...)
    private void broadcastUpdate(Salon salon) {
        messagingTemplate.convertAndSend(
                "/topic/salon/" + salon.getCode(),
                salonAssembler.toDto(salon)
        );
    }

    // Prévient les joueurs restés dans le salon que celui-ci vient d'être supprimé
    private void broadcastDeleted(String code) {
        messagingTemplate.convertAndSend("/topic/salon/" + code + "/deleted", "");
    }

    private boolean isUserInSalon(Long userId) {
        return salons.values().stream()
                .anyMatch(salon ->
                        salon.getUsers().stream()
                                .anyMatch(user -> user.getId().equals(userId)));
    }

    @Override
    public Salon create(Long userId) {

        if (isUserInSalon(userId)) {
            throw new ServiceException(
                    "L'utilisateur est déjà membre d'un salon."
            );
        }

        User user = userService.findById(userId)
                .orElseThrow(() ->
                        new ServiceException(
                                "L'utilisateur avec l'ID " + userId + " n'existe pas."
                        )
                );

        Salon salon = new Salon();

        salon.setCreateurId(userId);
        salon.setCode(generateCode());
        salon.setEtat(EtatSalon.OUVERT);

        salon.addUser(user);

        salons.put(salon.getCode(), salon);

        return salon;
    }

    @Override
    public Salon findByCode(String code) {

        Salon salon = salons.get(code);

        if (salon == null) {
            throw new ServiceException(
                    "Le salon avec le code " + code + " n'existe pas."
            );
        }

        return salon;
    }

    @Override
    public Salon addUser(String code, Long userId) {

        Salon salon = findByCode(code);

        if (isUserInSalon(userId)) {
            throw new ServiceException(
                    "L'utilisateur est déjà membre d'un salon."
            );
        }

        if (salon.getEtat() != EtatSalon.OUVERT) {
            throw new ServiceException(
                    "Impossible de rejoindre un salon qui a déjà commencé."
            );
        }

        User user = userService.findById(userId)
                .orElseThrow(() ->
                        new ServiceException(
                                "L'utilisateur avec l'ID " + userId + " n'existe pas."
                        )
                );

        salon.addUser(user);

        broadcastUpdate(salon);

        return salon;
    }

    @Override
    public Salon removeUser(String code, Long userId) {
        Salon salon = findByCode(code);

        // Le créateur quitte => suppression du salon pour tout le monde
        if (salon.getCreateurId().equals(userId)) {
            salons.remove(code);
            broadcastDeleted(code);
            return null;
        }

        User user = userService.findById(userId)
                .orElseThrow(() ->
                        new ServiceException("Utilisateur introuvable.")
                );

        salon.removeUser(user);

        broadcastUpdate(salon);

        return salon;
    }

    @Override
    public Salon changeEtat(String code, EtatSalon etat) {

        Salon salon = findByCode(code);

        salon.setEtat(etat);

        return salon;
    }

    @Override
    public Salon start(String code, Long userId, Long quizId) {

        Salon salon = findByCode(code);
        if (!salon.getCreateurId().equals(userId)) {
            throw new ServiceException(
                    "Seul le créateur du salon peut lancer la partie."
            );
        }

        // 2. Vérifier que le salon est encore ouvert
        if (salon.getEtat() != EtatSalon.OUVERT) {
            throw new ServiceException(
                    "La partie a déjà été lancée."
            );
        }

        // 3. Vérifier que le quiz existe
        Quiz quiz = quizService.findById(quizId)
                .orElseThrow(() ->
                        new ServiceException(
                                "Le quiz avec l'ID " + quizId + " n'existe pas."
                        )
                );

        // 4. Associer le quiz au salon
        salon.setQuizId(quiz.getId());

        // 5. Démarrer la partie
        salon.setEtat(EtatSalon.EN_COURS);

        broadcastUpdate(salon);

        return salon;
    }

    private String generateCode() {

        String code;

        do {
            code = String.format(
                    "%04d",
                    ThreadLocalRandom.current().nextInt(10000)
            );
        } while (salons.containsKey(code));

        return code;
    }

    @Override
    public void delete(String code, Long userId) {

        Salon salon = findByCode(code);

        if (!salon.getCreateurId().equals(userId)) {
            throw new ServiceException(
                    "Seul le créateur du salon peut le supprimer."
            );
        }

        salons.remove(code);
        broadcastDeleted(code);
    }
}