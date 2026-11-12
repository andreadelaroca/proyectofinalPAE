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
            return repository.findAll(session);
        }
    }
}