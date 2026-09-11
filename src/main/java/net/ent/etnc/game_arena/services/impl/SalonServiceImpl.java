package net.ent.etnc.game_arena.services.impl;
import net.ent.etnc.game_arena.models.entities.Salon;
import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.models.enumerations.EtatSalon;
import net.ent.etnc.game_arena.services.SalonService;
import net.ent.etnc.game_arena.services.UserService;
import net.ent.etnc.game_arena.services.commons.ServiceException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SalonServiceImpl implements SalonService {

    private final Map<String, Salon> salons = new ConcurrentHashMap<>();
    private final UserService userService;

    public SalonServiceImpl(UserService userService) {
        this.userService = userService;
    }

    private boolean isUserInSalon(Long userId) {
        return salons.values().stream()
                .anyMatch(salon ->
                        salon.getUsers().stream()
                                .anyMatch(user -> user.getId().equals(userId)));
    }

    @Override
    public Salon create(Long userId) {
        if (isUserInSalon(userId)) {throw new ServiceException("L'utilisateur est déjà membre d'un salon.");}
        User user = userService.findById(userId)
                .orElseThrow(() -> new ServiceException("L'utilisateur avec l'ID " + userId + " n'existe pas."));
        Salon salon = new Salon();
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
            throw new ServiceException("Le salon avec le code " + code + " n'existe pas.");
        }
        return salon;
    }

    @Override
    public Salon addUser(String code, Long userId) {
        Salon salon = findByCode(code);
        if (isUserInSalon(userId)) {throw new ServiceException("L'utilisateur est déjà membre d'un salon.");}
        User user = userService.findById(userId)
                .orElseThrow(() -> new ServiceException("L'utilisateur avec l'ID " + userId + " n'existe pas."));
        salon.addUser(user);
        return salon;
    }

    @Override
    public Salon removeUser(String code, Long userId) {
        Salon salon = findByCode(code);
        User user = userService.findById(userId)
                .orElseThrow(() -> new ServiceException("L'utilisateur avec l'ID " + userId + " n'existe pas."));
        salon.removeUser(user);
        return salon;
    }

    private String generateCode() {
        String code;
        do {
            code = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        } while (salons.containsKey(code));

        return code;
    }

    @Override
    public Salon changeEtat(String code, EtatSalon etat) {
        Salon salon = findByCode(code);
        salon.setEtat(etat);
        return salon;
    }

    @Override
    public void delete(String code) {
        if (salons.remove(code) == null) {
            throw new ServiceException("Le salon avec le code " + code + " n'existe pas.");
        }
    }
}