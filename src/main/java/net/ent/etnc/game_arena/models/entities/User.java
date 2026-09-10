package net.ent.etnc.game_arena.models.entities;

import jakarta.persistence.*;
import lombok.*;
import net.ent.etnc.game_arena.models.commons.AbstractPersistableWithIdSetter;

@Entity
@Table(name = "USER",
        uniqueConstraints = @UniqueConstraint(name = "uk_USER_nom", columnNames = {"nom"}))
@EqualsAndHashCode(callSuper = false, of = {"nom"})
@ToString(callSuper = true, of = {"nom"})
public class User extends AbstractPersistableWithIdSetter<Long> {

}