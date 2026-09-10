package net.ent.etnc.game_arena.repositories.commons;

import net.ent.etnc.jurassicpark.models.commons.AbstractPersistableWithIdSetter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Repository de base pour toutes les entités du projet.
 *
 * RÈGLE : Tous les repositories étendent BaseRepository, jamais JpaRepository directement.
 *
 * @NoRepositoryBean indique à Spring Data de ne PAS créer de bean pour cette interface
 * (c'est une interface abstraite, pas un repository concret)
 */
@NoRepositoryBean
public interface BaseRepository<T extends AbstractPersistableWithIdSetter<Long>> extends JpaRepository<T, Long>, JpaSpecificationExecutor<T> {
}
