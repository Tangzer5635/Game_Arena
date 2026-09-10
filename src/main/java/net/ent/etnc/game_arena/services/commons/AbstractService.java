package net.ent.etnc.game_arena.services.commons;

import jakarta.validation.groups.Default;
import net.ent.etnc.game_arena.models.commons.AbstractPersistableWithIdSetter;
import net.ent.etnc.game_arena.repositories.commons.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

/**
 * Implémentation générique des opérations CRUD communes à tous les services.
 *
 * Pattern : les services concrets ÉTENDENT AbstractService et SURCHARGENT
 * create()/update()/deleteById() pour y ajouter les règles métier spécifiques,
 * en appelant super.create() / super.update() à la fin.
 *
 * @Validated active la validation Bean Validation sur les méthodes de cette classe.
 * Les groupes Default+CreateGroup sont activés pour create(), Default+UpdateGroup pour update().
 */
@Validated
public class AbstractService<T extends AbstractPersistableWithIdSetter<Long>, R extends BaseRepository<T>>
        implements Service<T, Long> {

    // Le repository est protégé pour être accessible dans les sous-classes
    protected final R repository;

    public AbstractService(R repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    @Validated({Default.class})
    public T create(T entity) throws ServiceException {
        try {
            return repository.save(entity);
        } catch (Exception e) {
            throw new ServiceException("Erreur lors de la création de l'entité", e);
        }
    }

    @Override
    @Transactional
    @Validated({Default.class})
    public T update(T entity) throws ServiceException {
        if (entity.getId() == null) {
            throw new ServiceException("L'ID de l'entité ne peut pas être null pour une mise à jour.");
        }
        if (!repository.existsById(entity.getId())) {
            throw new ServiceException("L'entité avec l'ID " + entity.getId() + " n'existe pas.");
        }
        try {
            return repository.save(entity);
        } catch (Exception e) {
            throw new ServiceException("Erreur lors de la mise à jour de l'entité", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findById(Long id) throws ServiceException {
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<T> findAll(Pageable pageable) throws ServiceException {
        return repository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<T> findAll(Specification<T> spec, Pageable pageable) throws ServiceException {
        return repository.findAll(spec, pageable);
    }


    @Override
    @Transactional
    public void deleteById(Long id) throws ServiceException {
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public void delete(T entity) throws ServiceException {
        repository.delete(entity);
    }

    @Override
    @Transactional
    public void deleteAll() throws ServiceException {
        repository.deleteAll();
    }

    @Override
    @Transactional(readOnly = true)
    public long count() throws ServiceException {
        return repository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) throws ServiceException {
        return repository.existsById(id);
    }
}
