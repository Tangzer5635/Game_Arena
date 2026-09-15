package net.ent.etnc.game_arena.models.commons;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.jpa.domain.AbstractPersistable;

import java.io.Serializable;

/**
 * Classe de base pour toutes les entités JPA du projet.
 *
 * Apports par rapport à {@link AbstractPersistable} :
 * - Rend le setter d'ID public (nécessaire pour l'assembleur lors d'un update)
 * - Ajoute un champ {@code version} pour le contrôle de concurrence optimiste
 *   (si deux utilisateurs modifient le même enregistrement simultanément,
 *    Hibernate lève une OptimisticLockException sur le second sauvegarde)
 *
 * RÈGLE : Toutes les entités étendent cette classe, jamais directement @Entity avec @Id.
 */
@MappedSuperclass
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class AbstractPersistableWithIdSetter<PK extends Serializable> extends AbstractPersistable<PK> {

    // Rend public le setId() qui est protected dans AbstractPersistable
    @Override
    public void setId(PK id) {
        super.setId(id);
    }

    // @Version : Hibernate incrémente ce champ à chaque UPDATE
    // La valeur est envoyée dans le WHERE pour détecter les modifications concurrentes
    @Version
    @Column(name = "version", nullable = false)
    @Getter
    @Setter
    private Long version;
}
