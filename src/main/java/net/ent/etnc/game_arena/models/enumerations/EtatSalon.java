package net.ent.etnc.game_arena.models.enumerations;

import lombok.Getter;

@Getter
public enum EtatSalon {
    OUVERT("Ouvert"),
    EN_COURS("En cours");

    private final String etat;

    EtatSalon(String etat) {
        this.etat = etat;
    }

}
