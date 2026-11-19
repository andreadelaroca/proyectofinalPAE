package ni.edu.uam.proyectofinalpae.service;

import ni.edu.uam.proyectofinalpae.model.DetalleVenta;
import ni.edu.uam.proyectofinalpae.model.EstadoVenta;
import ni.edu.uam.proyectofinalpae.repository.GenericRepository;
import ni.edu.uam.proyectofinalpae.util.Tx;

import java.util.List;

public class DetalleVentaService {
    private final GenericRepository<DetalleVenta, Long> repository = new GenericRepository<>(DetalleVenta.class);

    public DetalleVenta buscarPorId(Long id) {
        return Tx.read(s -> repository.findById(s, id));
    }

    public List<DetalleVenta> obtenerPorVenta(Long idVenta) {
        return Tx.read(s -> s.createQuery(
                        "SELECT d FROM DetalleVenta d JOIN FETCH d.producto WHERE d.venta.id = :id ORDER BY d.id",
                        DetalleVenta.class)
                .setParameter("id", idVenta).list());
    }

    public List<Object[]> productosMasVendidos(int limite) {
        return Tx.read(s -> s.createQuery(
                        "SELECT d.producto.nombre, SUM(d.cantidad) FROM DetalleVenta d JOIN d.venta v " +
                                "WHERE v.estado = :estado GROUP BY d.producto.nombre ORDER BY SUM(d.cantidad) DESC",
                        Object[].class)
                .setParameter("estado", EstadoVenta.COMPLETADA)
                .setMaxResults(limite).list());
    }
}