package net.ent.etnc.game_arena.services.commons;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

/**
 * Interface générique de service CRUD.
 *
 * T  = type de l'entité (ex. Espece, Animal...)
 * ID = type de la clé primaire (Long)
 *
 * @Valid sur les paramètres active la validation Bean Validation
 * (les annotations @NotBlank, @NotNull, etc. des entités sont vérifiées
 *  avant l'exécution de la méthode du service)
 */
public interface Service<T, ID> {

    T create(@Valid T entity) throws ServiceException;

    T update(@Valid T entity) throws ServiceException;

    Optional<T> findById(ID id) throws ServiceException;

    Page<T> findAll(Pageable pageable) throws ServiceException;

    Page<T> findAll(Specification<T> spec, Pageable pageable) throws ServiceException;

    void deleteById(ID id) throws ServiceException;

    void delete(T entity) throws ServiceException;

    void deleteAll() throws ServiceException;

    long count() throws ServiceException;

    boolean existsById(ID id) throws ServiceException;
}
