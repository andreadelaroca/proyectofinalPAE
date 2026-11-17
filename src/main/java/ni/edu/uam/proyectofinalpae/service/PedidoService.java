package ni.edu.uam.proyectofinalpae.service;

import ni.edu.uam.proyectofinalpae.config.HibernateUtil;
import ni.edu.uam.proyectofinalpae.model.Pedido;
import ni.edu.uam.proyectofinalpae.repository.GenericRepository;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class PedidoService {
    private final GenericRepository<Pedido, Long> repository;

    public PedidoService() {
        this.repository = new GenericRepository<>(Pedido.class);
    }

    public void registrarPedido(Pedido pedido) {
        Transaction tx = null;
        try (Session session = HibernateUtil.factory().openSession()) {
            tx = session.beginTransaction();
            repository.save(session, pedido);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Error al registrar el pedido", e);
        }
    }

    public Pedido buscarPorId(Long id) {
        try (Session session = HibernateUtil.factory().openSession()) {
            return repository.findById(session, id);
        }
    }

    public List<Pedido> obtenerTodos() {
        try (Session session = HibernateUtil.factory().openSession()) {
            return session.createQuery(
                    "SELECT p FROM Pedido p " +
                            "JOIN FETCH p.cliente " +
                            "JOIN FETCH p.colaborador " +
                            "ORDER BY p.id DESC", Pedido.class
            ).list();
        }
    }

    public void actualizarEstado(Long idPedido, String nuevoEstado) {
        Transaction tx = null;
        try (Session session = HibernateUtil.factory().openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery(
                            "UPDATE Pedido p SET p.estado = :estado WHERE p.id = :id")
                    .setParameter("estado", nuevoEstado)
                    .setParameter("id", idPedido)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Error al actualizar el estado del pedido", e);
        }
    }
}