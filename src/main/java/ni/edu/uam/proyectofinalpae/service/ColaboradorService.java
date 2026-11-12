package ni.edu.uam.proyectofinalpae.service;

import ni.edu.uam.proyectofinalpae.config.HibernateUtil;
import ni.edu.uam.proyectofinalpae.model.Colaborador;
import ni.edu.uam.proyectofinalpae.repository.GenericRepository;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.List;

public class ColaboradorService {
    private final GenericRepository<Colaborador, Long> repository;

    public ColaboradorService() {
        this.repository = new GenericRepository<>(Colaborador.class);
    }

    public void guardar(Colaborador colaborador) {
        Transaction tx = null;
        try (Session session = HibernateUtil.factory().openSession()) {
            tx = session.beginTransaction();
            repository.save(session, colaborador);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Error al guardar el colaborador", e);
        }
    }

    public void actualizar(Colaborador colaborador) {
        Transaction tx = null;
        try (Session session = HibernateUtil.factory().openSession()) {
            tx = session.beginTransaction();
            repository.update(session, colaborador);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Error al actualizar el colaborador", e);
        }
    }

    public void eliminar(Colaborador colaborador) {
        Transaction tx = null;
        try (Session session = HibernateUtil.factory().openSession()) {
            tx = session.beginTransaction();
            repository.delete(session, colaborador);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Error al eliminar el colaborador", e);
        }
    }

    public Colaborador buscarPorId(Long id) {
        try (Session session = HibernateUtil.factory().openSession()) {
            return repository.findById(session, id);
        }
    }

    public List<Colaborador> obtenerTodos() {
        try (Session session = HibernateUtil.factory().openSession()) {
            return repository.findAll(session);
        }
    }
}