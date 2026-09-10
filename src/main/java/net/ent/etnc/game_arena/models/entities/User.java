package net.ent.etnc.game_arena.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.ent.etnc.game_arena.models.commons.AbstractPersistableWithIdSetter;
import net.ent.etnc.game_arena.models.enumerations.Role;
import org.hibernate.validator.constraints.Length;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Entity
@Table(name = "USERS",
        uniqueConstraints = @UniqueConstraint(name = "uk_USER_username", columnNames = {"username"}))
@EqualsAndHashCode(callSuper = false, of = {"username"})
@ToString(callSuper = true, of = {"username", "role"})
public class User extends AbstractPersistableWithIdSetter<Long> implements UserDetails {

    @Getter
    @Setter
    @NotBlank(message = "Le nom d'utilisateur est obligatoire.")
    @Column(name = "username", nullable = false, length = 50)
    private String username;

    // Le mot de passe est haché avant d'être stocké (BCrypt dans Init.java)
    @Getter
    @Setter
    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Column(name = "password", nullable = false)
    private String password;

    @Getter
    @Setter
    @NotNull(message = "Le rôle est obligatoire.")
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role.getAuthorities();
    }

    // --- Méthodes UserDetails pour Spring Security ---

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

}