package net.ent.etnc.game_arena.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.game_arena.dtos.assemblers.SalonAssembler;
import net.ent.etnc.game_arena.models.entities.SalonEntity;
import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.models.enumerations.EtatSalon;
import net.ent.etnc.game_arena.repositories.SalonRepository;
import net.ent.etnc.game_arena.services.SalonService;
import net.ent.etnc.game_arena.services.UserService;
import net.ent.etnc.game_arena.services.QuizService;
import net.ent.etnc.game_arena.services.commons.ServiceException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service gérant le cycle de vie des salons de jeu.
 * <p>
 * Toutes les opérations sur les salons sont maintenant persistées en base
 * PostgreSQL via {@link SalonRepository}, ce qui garantit la cohérence
 * même après un redémarrage du backend (essentiel pour la conteneurisation).
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SalonServiceImpl implements SalonService {

    private final SalonRepository salonRepository;
    private final UserService userService;
    private final QuizService quizService;
    private final SalonAssembler salonAssembler;
    private final SimpMessagingTemplate messagingTemplate;

    // ── Broadcasts ────────────────────────────────────────────────────────────

    /**
     * Notifie tous les joueurs du salon d'un changement d'état
     * (joueur rejoint/quitte, statut modifié).
     */
    private void broadcastUpdate(SalonEntity salon) {
        messagingTemplate.convertAndSend(
                "/topic/salon/" + salon.getCode(),
                salonAssembler.toDto(salon)
        );
    }

    /**
     * Notifie les joueurs restants que le salon a été supprimé
     * (hôte parti ou partie terminée).
     */
    private void broadcastDeleted(String code) {
        messagingTemplate.convertAndSend("/topic/salon/" + code + "/deleted", "");
    }

    // ── Règles métier ─────────────────────────────────────────────────────────

    /**
     * Crée un nouveau salon.
     * <p>
     * Règles :
     * <ul>
     *   <li>{@code maxPlayers} doit être une puissance de 2 entre 2 et 16 inclus</li>
     *   <li>Si l'utilisateur est déjà dans un salon actif, ce salon est retourné
     *       (idempotent — évite le 400 après un refresh ou un redémarrage backend)</li>
     * </ul>
     *
     * @param userId     ID de l'utilisateur créateur
     * @param maxPlayers limite de joueurs (2, 4, 8 ou 16)
     * @return salon créé ou salon existant si l'utilisateur y était déjà
     */
    @Override
    public SalonEntity create(Long userId, int maxPlayers) {
        if (maxPlayers < 2 || maxPlayers > 16 || (maxPlayers & (maxPlayers - 1)) != 0) {
            throw new ServiceException("La limite de joueurs doit être 2, 4, 8 ou 16.");
        }

        // Idempotence : si l'utilisateur a déjà un salon actif, on le renvoie
        Optional<SalonEntity> existing = salonRepository.findActiveSalonByUserId(userId);
        if (existing.isPresent()) {
            log.debug("Utilisateur {} déjà dans le salon {}", userId, existing.get().getCode());
            return existing.get();
        }

        User user = userService.findById(userId)
                .orElseThrow(() -> new ServiceException("Utilisateur introuvable : " + userId));

        SalonEntity salon = new SalonEntity();
        salon.setCreateurId(userId);
        salon.setCode(generateUniqueCode());
        salon.setEtat(EtatSalon.OUVERT);
        salon.setMaxPlayers(maxPlayers);
        salon.addUser(user);

        SalonEntity saved = salonRepository.save(salon);
        log.info("Salon {} créé par {}", saved.getCode(), userId);
        return saved;
    }

    /**
     * Recherche un salon par son code à 4 chiffres.
     *
     * @throws ServiceException si le code est inconnu
     */
    @Override
    @Transactional(readOnly = true)
    public SalonEntity findByCode(String code) {
        return salonRepository.findByCode(code)
                .orElseThrow(() -> new ServiceException("Salon introuvable : " + code));
    }

    /**
     * Fait rejoindre un utilisateur dans un salon.
     * <p>
     * Règles :
     * <ul>
     *   <li>Si déjà membre de CE salon → no-op (idempotent)</li>
     *   <li>Si membre d'un AUTRE salon → exception</li>
     *   <li>Salon complet → exception</li>
     *   <li>Salon non OUVERT → exception</li>
     * </ul>
     */
    @Override
    public SalonEntity addUser(String code, Long userId) {
        SalonEntity salon = findByCode(code);

        if (salon.getUsers().stream().anyMatch(u -> u.getId().equals(userId))) {
            return salon; // idempotent
        }

        if (salonRepository.existsByUserIdAndNotTermine(userId)) {
            throw new ServiceException("L'utilisateur est déjà membre d'un autre salon actif.");
        }

        if (salon.getUsers().size() >= salon.getMaxPlayers()) {
            throw new ServiceException("Le salon est complet (" + salon.getMaxPlayers() + " joueurs max).");
        }

        if (salon.getEtat() != EtatSalon.OUVERT) {
            throw new ServiceException("Impossible de rejoindre un salon qui a déjà commencé.");
        }

        User user = userService.findById(userId)
                .orElseThrow(() -> new ServiceException("Utilisateur introuvable : " + userId));

        salon.addUser(user);
        SalonEntity saved = salonRepository.save(salon);
        broadcastUpdate(saved);
        return saved;
    }

    /**
     * Fait quitter un utilisateur du salon.
     * <p>
     * Règle : si l'utilisateur est le créateur (hôte), le salon entier est
     * supprimé et tous les joueurs restants sont notifiés.
     */
    @Override
    public SalonEntity removeUser(String code, Long userId) {
        SalonEntity salon = findByCode(code);

        if (salon.getCreateurId().equals(userId)) {
            salonRepository.delete(salon);
            broadcastDeleted(code);
            log.info("Salon {} supprimé par son hôte ({})", code, userId);
            return null;
        }

        User user = userService.findById(userId)
                .orElseThrow(() -> new ServiceException("Utilisateur introuvable : " + userId));

        salon.removeUser(user);
        SalonEntity saved = salonRepository.save(salon);
        broadcastUpdate(saved);
        return saved;
    }

    /**
     * Lance officiellement la partie dans un salon.
     * <p>
     * Règles :
     * <ul>
     *   <li>Seul le créateur peut lancer</li>
     *   <li>Le salon doit être OUVERT</li>
     *   <li>Le quiz désigné doit exister</li>
     * </ul>
     *
     * @param code   code du salon
     * @param userId ID de l'utilisateur qui demande le lancement (doit être le créateur)
     * @param quizId ID du quiz choisi
     */
    @Override
    public SalonEntity start(String code, Long userId, Long quizId) {
        SalonEntity salon = findByCode(code);

        if (!salon.getCreateurId().equals(userId)) {
            throw new ServiceException("Seul le créateur du salon peut lancer la partie.");
        }

        if (salon.getEtat() != EtatSalon.OUVERT) {
            throw new ServiceException("La partie a déjà été lancée.");
        }

        quizService.findById(quizId)
                .orElseThrow(() -> new ServiceException("Quiz introuvable : " + quizId));

        salon.setQuizId(quizId);
        salon.setEtat(EtatSalon.EN_COURS);
        salon.touch();

        SalonEntity saved = salonRepository.save(salon);
        broadcastUpdate(saved);
        return saved;
    }

    /**
     * Supprime un salon à la demande explicite de son créateur.
     *
     * @throws ServiceException si l'utilisateur n'est pas le créateur
     */
    @Override
    public void delete(String code, Long userId) {
        SalonEntity salon = findByCode(code);

        if (!salon.getCreateurId().equals(userId)) {
            throw new ServiceException("Seul le créateur du salon peut le supprimer.");
        }

        salonRepository.delete(salon);
        broadcastDeleted(code);
    }

    /**
     * Supprime silencieusement un salon en fin de partie, sans vérification
     * d'autorisation (appelé par {@code GameServiceImpl} uniquement).
     */
    @Override
    public void cleanup(String code) {
        salonRepository.findByCode(code).ifPresent(salon -> {
            salonRepository.delete(salon);
            log.debug("Salon {} nettoyé après fin de partie", code);
        });
    }

    /**
     * Retire un joueur de tout salon actif où il se trouve.
     * Appelé automatiquement par {@link net.ent.etnc.game_arena.config.WebSocketDisconnectListener}
     * quand un WebSocket se ferme inopinément.
     */
    @Override
    public void removeUserFromAnySalon(Long userId) {
        salonRepository.findActiveSalonByUserId(userId).ifPresent(salon -> {
            if (salon.getCreateurId().equals(userId)) {
                salonRepository.delete(salon);
                broadcastDeleted(salon.getCode());
                log.info("Salon {} supprimé suite à déconnexion de l'hôte ({})", salon.getCode(), userId);
            } else {
                userService.findById(userId).ifPresent(u -> {
                    salon.removeUser(u);
                    salonRepository.save(salon);
                    broadcastUpdate(salon);
                });
            }
        });
    }

    @Override
    public SalonEntity changeEtat(String code, EtatSalon etat) {
        SalonEntity salon = findByCode(code);
        salon.setEtat(etat);
        return salonRepository.save(salon);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Génère un code à 4 chiffres unique parmi les salons actifs en base.
     * Boucle jusqu'à trouver un code libre (en pratique : < 2 itérations).
     */
    private String generateUniqueCode() {
        String code;
        do {
            code = String.format("%04d", ThreadLocalRandom.current().nextInt(10_000));
        } while (salonRepository.findByCode(code).isPresent());
        return code;
    }
}
