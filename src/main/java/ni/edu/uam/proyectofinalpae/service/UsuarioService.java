package ni.edu.uam.proyectofinalpae.service;

import ni.edu.uam.proyectofinalpae.config.HibernateUtil;
import ni.edu.uam.proyectofinalpae.model.Usuario;
import ni.edu.uam.proyectofinalpae.repository.GenericRepository;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class UsuarioService {
    private final GenericRepository<Usuario, Long> repository;

    public UsuarioService() {
        this.repository = new GenericRepository<>(Usuario.class);
    }

    public void guardar(Usuario usr) {
        Transaction tx = null;
        try (Session s = HibernateUtil.factory().openSession()) {
            tx = s.beginTransaction();
            repository.save(s, usr);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Error al guardar el usuario", e);
        }
    }

    public void actualizar(Usuario usr) {
        Transaction tx = null;
        try (Session s = HibernateUtil.factory().openSession()) {
            tx = s.beginTransaction();
            repository.update(s, usr);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Error al actualizar al cliente", e);
        }
    }

    public Usuario buscarPorId(Long id) {
        try (Session s = HibernateUtil.factory().openSession()) {
            return repository.findById(s, id);
        }
    }

    public List<Usuario> obtenerTodos() {
        try (Session s = HibernateUtil.factory().openSession()) {
            return repository.findAll(s);
        }
    }

}
