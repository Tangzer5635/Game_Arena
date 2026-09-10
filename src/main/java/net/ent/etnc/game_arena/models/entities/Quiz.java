package net.ent.etnc.game_arena.models.entities;

import io.github.perplexhub.rsql.Q;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.ent.etnc.game_arena.models.commons.AbstractPersistableWithIdSetter;
import org.hibernate.validator.constraints.Length;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "QUIZ",
        uniqueConstraints = @UniqueConstraint(name = "uk_QUIZ_nom", columnNames = {"titre"}))
@EqualsAndHashCode(callSuper = false, of = {"titre"})
@ToString(callSuper = true, of = {"titre", "description"})
public class Quiz extends AbstractPersistableWithIdSetter<Long> {
    @Getter
    @Setter
    @NotNull(message = "titre ne doit pas être null")
    @NotEmpty(message = "titre ne doit pas être vide")
    @NotBlank(message = "titre doit contenir des caractères lisibles")
    @Length(min = 3, max = 50, message = "titre doit avoir entre 3 et 50 caractères")
    @Column(name = "titre", length = 50, nullable = false)
    private String titre;

    @Getter
    @Setter
    @NotNull(message = "description ne doit pas être null")
    @NotEmpty(message = "description ne doit pas être vide")
    @NotBlank(message = "description doit contenir des caractères lisibles")
    @Length(min = 3, max = 512, message = "description doit avoir entre 3 et 512 caractères")
    @Column(name = "description", length = 512, nullable = false)
    private String description;

    @Valid
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id",
            foreignKey = @ForeignKey(name = "fk_QUESTION_quiz"))
    private List<Question> questions = new ArrayList<>();

    public List<Question> getQuestions() {
        return Collections.unmodifiableList(questions);
    }

    public void addQuestion(Question question) {
        questions.add(question);
    }
    public void removeQuestion(Question question) {
        questions.remove(question);
    }
}