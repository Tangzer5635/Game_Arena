package net.ent.etnc.game_arena.models.entities;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.ent.etnc.game_arena.models.commons.AbstractPersistableWithIdSetter;
import net.ent.etnc.game_arena.models.enumerations.TypeQuestion;
import org.hibernate.validator.constraints.Length;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "QUESTION")
@ToString(callSuper = true, of = {"text"})
public class Question extends AbstractPersistableWithIdSetter<Long> {
    @Getter
    @Setter
    @NotNull(message = "text ne doit pas être null")
    @NotEmpty(message = "text ne doit pas être vide")
    @NotBlank(message = "text doit contenir des caractères lisibles")
    @Length(min = 3, max = 512, message = "text doit avoir entre 3 et 512 caractères")
    @Column(name = "text", length = 512, nullable = false)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Getter
    @Setter
    private TypeQuestion type = TypeQuestion.CHOIX;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "question_id", foreignKey = @ForeignKey(name = "fk_REPONSE_question"))
    private List<Reponse> reponses = new ArrayList<>();
    
    public List<Reponse> getReponses() {
        return Collections.unmodifiableList(reponses);
    }
    
    public void addReponse(Reponse reponse) {
        reponses.add(reponse);
    }
    public void removeReponse(Reponse reponse) {
        reponses.remove(reponse);
    }

}