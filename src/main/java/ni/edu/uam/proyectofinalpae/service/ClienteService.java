package ni.edu.uam.proyectofinalpae.service;

import ni.edu.uam.proyectofinalpae.config.HibernateUtil;
import ni.edu.uam.proyectofinalpae.model.Cliente;
import ni.edu.uam.proyectofinalpae.repository.GenericRepository;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class ClienteService {
    private final GenericRepository<Cliente, Long> repository;

    public ClienteService() {
        this.repository = new GenericRepository<>(Cliente.class);
    }

    public void guardar(Cliente cliente) {
        Transaction tx = null;
        try (Session session = HibernateUtil.factory().openSession()) {
            tx = session.beginTransaction();
            repository.save(session, cliente);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Error al guardar el cliente", e);
        }
    }

    public void actualizar(Cliente cliente) {
        Transaction tx = null;
        try (Session session = HibernateUtil.factory().openSession()) {
            tx = session.beginTransaction();
            repository.update(session, cliente);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Error al actualizar el cliente", e);
        }
    }

    public Cliente buscarPorId(Long id) {
        try (Session session = HibernateUtil.factory().openSession()) {
            return repository.findById(session, id);
        }
    }

    public List<Cliente> obtenerTodos() {
        try (Session session = HibernateUtil.factory().openSession()) {
            return session.createQuery(
                    "SELECT DISTINCT c FROM Cliente c LEFT JOIN FETCH c.direcciones ORDER BY c.id ASC",
                    Cliente.class
            ).list();
        }
    }

    public boolean existeUsername(String username) {
        try (Session session = HibernateUtil.factory().openSession()) {
            Long count = session.createQuery(
                            "SELECT COUNT(c.id) FROM Cliente c WHERE LOWER(c.username) = LOWER(:user)",
                            Long.class)
                    .setParameter("user", username.trim())
                    .uniqueResult();
            return count != null && count > 0;
        }
    }
}