package ni.edu.uam.proyectofinalpae.service;

import ni.edu.uam.proyectofinalpae.config.HibernateUtil;
import ni.edu.uam.proyectofinalpae.model.*;
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

            // Verifica y descuenta el inventario de cada producto en la misma transacción
            for (DetallePedido detalle : pedido.getDetalles()) {
                Producto prodBD = session.get(Producto.class, detalle.getProducto().getId());
                if (prodBD == null) {
                    throw new IllegalStateException("El producto ya no existe en la base de datos.");
                }
                if (prodBD.getCantidad() < detalle.getCantidad()) {
                    throw new IllegalStateException("Stock insuficiente para " + prodBD.getCodigo()
                            + " (disponible: " + prodBD.getCantidad() + ").");
                }
                prodBD.setCantidad(prodBD.getCantidad() - detalle.getCantidad());
            }

            session.persist(pedido);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException(e.getMessage(), e);
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

    public List<Cliente> obtenerClientes() {
        try (Session session = HibernateUtil.factory().openSession()) {
            return session.createQuery("FROM Cliente c ORDER BY c.nombres", Cliente.class).list();
        }
    }

    public List<Colaborador> obtenerColaboradores() {
        try (Session session = HibernateUtil.factory().openSession()) {
            return session.createQuery("FROM Colaborador c ORDER BY c.nombres", Colaborador.class).list();
        }
    }
}