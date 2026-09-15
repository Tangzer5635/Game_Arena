package net.ent.etnc.game_arena.models.entities;

import jakarta.persistence.*;
import lombok.*;
import net.ent.etnc.game_arena.models.commons.AbstractPersistableWithIdSetter;
import net.ent.etnc.game_arena.models.enumerations.EtatSalon;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Salon de jeu persisté en base PostgreSQL.
 * <p>
 * Remplace l'ancien POJO {@code Salon} stocké en RAM.
 * Toutes les informations du lobby (joueurs, état, code…) survivent
 * aux redémarrages du backend — essentiel pour la conteneurisation.
 * <p>
 * L'état live de la partie ({@link GameState}) est dans une table séparée
 * pour isoler les données qui changent à chaque milliseconde (scores, streaks)
 * des métadonnées du salon (code, hôte, quiz choisi).
 */
@Entity
@Table(name = "SALON",
        uniqueConstraints = @UniqueConstraint(name = "uk_SALON_code", columnNames = {"code"}))
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false, of = {"code"})
@ToString(of = {"code", "etat"})
public class SalonEntity extends AbstractPersistableWithIdSetter<Long> {

    /** Code à 4 chiffres partagé aux joueurs pour rejoindre. */
    @Column(nullable = false, length = 4)
    private String code;

    /** ID de l'utilisateur créateur (hôte). */
    @Column(nullable = false)
    private Long createurId;

    /** ID du quiz associé (null jusqu'à ce que l'hôte en choisisse un). */
    @Column
    private Long quizId;

    /** État courant du salon (OUVERT → EN_COURS → TERMINE). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EtatSalon etat = EtatSalon.OUVERT;

    /** Limite de joueurs configurée par l'hôte (2, 4, 8 ou 16). */
    @Column(nullable = false)
    private int maxPlayers = 8;

    /**
     * Joueurs actuellement dans le salon.
     * ManyToMany lazy : on charge la liste seulement quand nécessaire.
     * {@code cascade = PERSIST} : un User déjà persisté ne doit pas être recréé.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "SALON_USERS",
            joinColumns = @JoinColumn(name = "salon_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> users = new ArrayList<>();

    /**
     * Timestamp de dernière activité.
     * Utilisé par {@code SalonTtlScheduler} pour supprimer les salons abandonnés.
     */
    @Column(nullable = false)
    private Instant lastActivityAt = Instant.now();

    // ── Délégation list ───────────────────────────────────────────────────────

    public List<User> getUsers() {
        return Collections.unmodifiableList(users);
    }

    /**
     * Ajoute un joueur si pas déjà présent.
     *
     * @throws IllegalStateException si le salon est complet ou n'est pas OUVERT
     */
    public void addUser(User user) {
        if (!users.contains(user)) {
            users.add(user);
            touch();
        }
    }

    /** Retire un joueur du salon. */
    public void removeUser(User user) {
        users.remove(user);
        touch();
    }

    /** Met à jour le timestamp de dernière activité. */
    public void touch() {
        this.lastActivityAt = Instant.now();
    }
}
