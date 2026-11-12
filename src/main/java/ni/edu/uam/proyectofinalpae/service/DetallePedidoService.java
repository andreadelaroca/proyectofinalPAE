package ni.edu.uam.proyectofinalpae.service;

import ni.edu.uam.proyectofinalpae.config.HibernateUtil;
import ni.edu.uam.proyectofinalpae.model.DetallePedido;
import ni.edu.uam.proyectofinalpae.repository.GenericRepository;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.List;

public class DetallePedidoService {
    private final GenericRepository<DetallePedido, Long> repository;

    public DetallePedidoService() {
        this.repository = new GenericRepository<>(DetallePedido.class);
    }

    public void guardar(DetallePedido detalle) {
        Transaction tx = null;
        try (Session session = HibernateUtil.factory().openSession()) {
            tx = session.beginTransaction();
            repository.save(session, detalle);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Error al guardar el detalle del pedido", e);
        }
    }

    public void actualizar(DetallePedido detalle) {
        Transaction tx = null;
        try (Session session = HibernateUtil.factory().openSession()) {
            tx = session.beginTransaction();
            repository.update(session, detalle);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Error al actualizar el detalle del pedido", e);
        }
    }

    public void eliminar(DetallePedido detalle) {
        Transaction tx = null;
        try (Session session = HibernateUtil.factory().openSession()) {
            tx = session.beginTransaction();
            repository.delete(session, detalle);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Error al eliminar el detalle del pedido", e);
        }
    }

    public DetallePedido buscarPorId(Long id) {
        try (Session session = HibernateUtil.factory().openSession()) {
            return repository.findById(session, id);
        }
    }

    public List<DetallePedido> obtenerTodos() {
        try (Session session = HibernateUtil.factory().openSession()) {
            return repository.findAll(session);
        }
    }
}