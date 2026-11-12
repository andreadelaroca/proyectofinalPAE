package ni.edu.uam.proyectofinalpae.repository;

import org.hibernate.Session;
import java.util.List;

public class GenericRepository<T, ID> {
    private final Class<T> entityClass;

    public GenericRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public void save(Session session, T entity) {
        session.persist(entity);
    }

    public void update(Session session, T entity) {
        session.merge(entity);
    }

    public void delete(Session session, T entity) {
        session.remove(entity);
    }

    public T findById(Session session, ID id) {
        return session.get(entityClass, id);
    }

    public List<T> findAll(Session session) {
        return session.createQuery("from " + entityClass.getName(), entityClass).list();
    }
}