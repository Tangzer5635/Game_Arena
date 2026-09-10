package net.ent.etnc.game_arena.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.ent.etnc.game_arena.models.commons.AbstractPersistableWithIdSetter;
import org.hibernate.validator.constraints.Length;

@Entity
@Table(name = "REPONSE")
@ToString(callSuper = true, of = {"text", "est_bonne"})
public class Reponse extends AbstractPersistableWithIdSetter<Long> {
    @Getter
    @Setter
    @NotNull(message = "text ne doit pas être null")
    @NotEmpty(message = "text ne doit pas être vide")
    @NotBlank(message = "text doit contenir des caractères lisibles")
    @Length(min = 3, max = 512, message = "text doit avoir entre 3 et 512 caractères")
    @Column(name = "text", length = 512, nullable = false)
    private String text;

    @Getter
    @Setter
    @Column(name = "est_bonne", nullable = false)
    private boolean estBonne;
}