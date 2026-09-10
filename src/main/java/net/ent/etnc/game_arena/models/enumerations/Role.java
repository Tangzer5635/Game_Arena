package net.ent.etnc.game_arena.models.enumerations;

import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public enum Role {
    USER("User"),
    ADMIN("Administrateur", USER);

    private final String label;
    private final Set<Role> roles;

    Role(String label, Role... roles) {
        this.label = label;
        this.roles = roles.length > 0 ? Set.of(roles) : Collections.emptySet();
    }

    public Set<Role> getAllRoles() {
        Set<Role> allRoles = new HashSet<>();
        allRoles.add(this);
        for (Role parent : this.roles) {
            allRoles.addAll(parent.getAllRoles());
        }
        return allRoles;
    }

    public boolean hasRole(Role role) {
        return getAllRoles().contains(role);
    }

    public Collection<SimpleGrantedAuthority> getAuthorities() {
        return getAllRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toSet());
    }
}