package ni.edu.uam.proyectofinalpae.service;

import ni.edu.uam.proyectofinalpae.model.Producto;
import ni.edu.uam.proyectofinalpae.repository.GenericRepository;
import ni.edu.uam.proyectofinalpae.util.NegocioException;
import ni.edu.uam.proyectofinalpae.util.Tx;
import org.hibernate.LockMode;
import org.hibernate.Session;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProductoService {
    private final GenericRepository<Producto, Long> repository = new GenericRepository<>(Producto.class);

    public Producto registrarProducto(Producto producto) {
        return Tx.run("Error al registrar el producto", s -> {
            normalizarColor(producto);
            producto.setCodigo("TMP-" + UUID.randomUUID().toString().substring(0, 8)); // codigo es NOT NULL
            repository.save(s, producto);                                              // IDENTITY: ya tiene id
            producto.setCodigo(String.format("BUN-%03d", producto.getId()));
            return producto;
        });
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
        return Tx.read(s -> {
            Long ultimoId = s.createQuery("SELECT COALESCE(MAX(p.id), 0L) FROM Producto p", Long.class).uniqueResult();
            return String.format("BUN-%03d", ultimoId + 1);
        });
    }

    public Producto actualizar(Producto producto) {
        return Tx.run("Error al actualizar el producto", s -> {
            normalizarColor(producto);
            return repository.update(s, producto);
        });
    }

    public void eliminar(Producto producto) {
        Tx.exec("No se pudo eliminar el producto (puede tener pedidos o ventas asociados)",
                s -> repository.delete(s, producto));
    }

    public Producto buscarPorId(Long id) {
        return Tx.read(s -> s.createQuery(
                        "FROM Producto p JOIN FETCH p.marca WHERE p.id = :id", Producto.class)
                .setParameter("id", id).uniqueResult());
    }

    public Producto buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) return null;
        return Tx.read(s -> s.createQuery(
                        "FROM Producto p JOIN FETCH p.marca WHERE UPPER(p.codigo) = UPPER(:codigo)", Producto.class)
                .setParameter("codigo", codigo.trim()).uniqueResult());
    }

    public List<Producto> obtenerTodos() {
        return Tx.read(s -> s.createQuery(
                "SELECT p FROM Producto p JOIN FETCH p.marca ORDER BY p.codigo", Producto.class).list());
    }

    static void descontarStock(Session s, Map<Long, Integer> cantidades) {
        cantidades.forEach((id, cant) -> {
            if (cant == null || cant <= 0) throw new NegocioException("La cantidad debe ser mayor que cero.");
            Producto p = bloquear(s, id);
            if (p.getCantidad() < cant) {
                throw new NegocioException("Stock insuficiente para " + p.getCodigo()
                        + " (disponible: " + p.getCantidad() + ").");
            }
            p.setCantidad(p.getCantidad() - cant);
        });
    }

    static void devolverStock(Session s, Map<Long, Integer> cantidades) {
        cantidades.forEach((id, cant) -> {
            Producto p = bloquear(s, id);
            p.setCantidad(p.getCantidad() + cant);
        });
    }

    private static Producto bloquear(Session s, Long id) {
        Producto p = s.get(Producto.class, id, LockMode.PESSIMISTIC_WRITE); // SELECT ... FOR UPDATE
        if (p == null) throw new NegocioException("El producto ya no existe en la base de datos.");
        return p;
    }

    private void normalizarColor(Producto p) {
        if (p.getColor() != null && p.getColor().startsWith("#")) {
            p.setColor(p.getColor().substring(1));
        }
    }
}