package net.ent.etnc.game_arena.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.ent.etnc.game_arena.models.commons.AbstractPersistableWithIdSetter;

@Entity
@Table(name = "REFRESHTOKEN",
        uniqueConstraints = @UniqueConstraint(name = "uk_REFRESHTOKEN_nom", columnNames = {"nom"}))
@EqualsAndHashCode(callSuper = false, of = {"nom"})
@ToString(callSuper = true, of = {"nom"})
public class RefreshToken extends AbstractPersistableWithIdSetter<Long> {

}