package net.ent.etnc.game_arena.models.entities;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import net.ent.etnc.game_arena.models.commons.AbstractPersistableWithIdSetter;
import net.ent.etnc.game_arena.models.enumerations.EtatSalon;
import org.hibernate.validator.constraints.Length;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@EqualsAndHashCode(callSuper = false, of = {"code"})
@ToString(callSuper = true, of = {"code"})
public class Salon extends AbstractPersistableWithIdSetter<Long> {
    
    @Getter
    @Setter
    @NotNull(message = "code ne doit pas être null")
    @NotEmpty(message = "code ne doit pas être vide")
    @NotBlank(message = "code doit contenir des caractères lisibles")
    @Pattern(regexp = "^[0-9]{4}$")
    private String code;
    
    @Valid
    private List<User> users = new ArrayList<>();

    public List<User> getUsers() {
        return Collections.unmodifiableList(users);
    }

    public void addUser(User user) {
        users.add(user);
    }
    public void removeUser(User user) {
        users.remove(user);
    }
    
    @Getter
    @Setter
    @NotNull(message = "etat ne doit pas être null")
    private EtatSalon etat;
}