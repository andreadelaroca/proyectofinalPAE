package ni.edu.uam.proyectofinalpae.service;

import ni.edu.uam.proyectofinalpae.model.DetalleVenta;
import ni.edu.uam.proyectofinalpae.model.EstadoVenta;
import ni.edu.uam.proyectofinalpae.repository.GenericRepository;
import ni.edu.uam.proyectofinalpae.util.Tx;

import java.util.List;

/**
 * Solo lectura a propósito: crear, editar o borrar líneas sueltas rompería el stock y los totales
 * de la venta. Las líneas se crean con Venta.agregarDetalle + VentaService.registrarVenta.
 */
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

    /** Para reportes/gráficos: filas [nombreProducto, unidadesVendidas], sin contar ventas anuladas. */
    public List<Object[]> productosMasVendidos(int limite) {
        return Tx.read(s -> s.createQuery(
                        "SELECT d.producto.nombre, SUM(d.cantidad) FROM DetalleVenta d JOIN d.venta v " +
                                "WHERE v.estado = :estado GROUP BY d.producto.nombre ORDER BY SUM(d.cantidad) DESC",
                        Object[].class)
                .setParameter("estado", EstadoVenta.COMPLETADA)
                .setMaxResults(limite).list());
    }

    /** Filas [nombreProducto, unidades] de ventas completadas en el rango de fechas (inclusive). */
    public List<Object[]> productosMasVendidos(java.time.LocalDate desde, java.time.LocalDate hasta, int limite) {
        return Tx.read(s -> s.createQuery(
                        "SELECT d.producto.nombre, SUM(d.cantidad) FROM DetalleVenta d JOIN d.venta v " +
                                "WHERE v.estado = :estado AND v.fecha >= :desde AND v.fecha < :hasta " +
                                "GROUP BY d.producto.nombre ORDER BY SUM(d.cantidad) DESC", Object[].class)
                .setParameter("estado", EstadoVenta.COMPLETADA)
                .setParameter("desde", desde.atStartOfDay())
                .setParameter("hasta", hasta.plusDays(1).atStartOfDay())
                .setMaxResults(limite).list());
    }
}