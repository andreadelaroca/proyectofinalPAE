package ni.edu.uam.proyectofinalpae.service;

import ni.edu.uam.proyectofinalpae.config.HibernateUtil;
import ni.edu.uam.proyectofinalpae.model.Producto;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class ProductoService {

    /**
     * Registra un nuevo producto asignándole automáticamente el código BUN-XXX
     * y devuelve el producto guardado con su ID y código generados.
     */
    public Producto registrarProducto(Producto producto) {
        Transaction tx = null;
        try (Session session = HibernateUtil.factory().openSession()) {
            tx = session.beginTransaction();

            Long ultimoId = session.createQuery(
                            "SELECT COALESCE(MAX(p.id), 0L) FROM Producto p", Long.class)
                    .uniqueResult();

            String nuevoCodigo = String.format("BUN-%03d", ultimoId + 1);
            producto.setCodigo(nuevoCodigo);

            // Asegura que el color hex no lleve '#' para respetar length = 6
            if (producto.getColor() != null && producto.getColor().startsWith("#")) {
                producto.setColor(producto.getColor().substring(1));
            }

            session.persist(producto);
            tx.commit();
            return producto;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Error al registrar el producto", e);
        }
    }

    /**
     * Actualiza los datos de un producto existente manteniendo su código BUN-XXX.
     */
    public void actualizarProducto(Producto producto) {
        Transaction tx = null;
        try (Session session = HibernateUtil.factory().openSession()) {
            tx = session.beginTransaction();

            if (producto.getColor() != null && producto.getColor().startsWith("#")) {
                producto.setColor(producto.getColor().substring(1));
            }

            session.merge(producto);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Error al actualizar el producto", e);
        }
    }

    /**
     * Útil si deseas previsualizar el próximo código en un TextField deshabilitado del formulario.
     */
    public String obtenerSiguienteCodigo() {
        try (Session session = HibernateUtil.factory().openSession()) {
            Long ultimoId = session.createQuery(
                            "SELECT COALESCE(MAX(p.id), 0L) FROM Producto p", Long.class)
                    .uniqueResult();
            return String.format("BUN-%03d", ultimoId + 1);
        }
    }

    public Producto buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        try (Session session = HibernateUtil.factory().openSession()) {
            return session.createQuery(
                            "FROM Producto p WHERE UPPER(p.codigo) = UPPER(:codigo)",
                            Producto.class)
                    .setParameter("codigo", codigo.trim())
                    .uniqueResult();
        }
    }

    public List<Producto> obtenerTodos() {
        try (Session session = HibernateUtil.factory().openSession()) {
            return session.createQuery("FROM Producto p ORDER BY p.codigo", Producto.class)
                    .list();
        }
    }
}