package ni.edu.uam.proyectofinalpae.service;

import ni.edu.uam.proyectofinalpae.model.*;
import ni.edu.uam.proyectofinalpae.repository.GenericRepository;
import ni.edu.uam.proyectofinalpae.util.NegocioException;
import ni.edu.uam.proyectofinalpae.util.Tx;
import org.hibernate.Session;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class VentaService {
    private final GenericRepository<Venta, Long> repository = new GenericRepository<>(Venta.class);

    /**
     * Registra una venta directa (o con pedido ya armado): valida, descuenta stock si no viene
     * de pedido, calcula totales y asigna el número de factura.
     */
    public Venta registrarVenta(Venta venta) {
        return Tx.run("Error al registrar la venta", s -> {
            if (venta.getDetalles().isEmpty()) throw new NegocioException("La venta debe tener al menos un producto.");
            if (venta.getColaborador() == null) throw new NegocioException("La venta debe tener un colaborador.");

            if (venta.getPedido() == null) {
                ProductoService.descontarStock(s, cantidadesPorProducto(venta));
            } else {
                Pedido pedido = s.get(Pedido.class, venta.getPedido().getId());
                if (pedido == null) throw new NegocioException("El pedido no existe.");
                if (pedido.getEstado() == EstadoPedido.CANCELADO) throw new NegocioException("El pedido está cancelado.");
                if (pedido.getEstado() == EstadoPedido.ENTREGADO) throw new NegocioException("El pedido ya fue facturado y entregado.");
                pedido.setEstado(EstadoPedido.ENTREGADO);
                venta.setPedido(pedido);
            }

            venta.recalcularTotales();
            venta.setNumeroFactura("TMP-" + UUID.randomUUID().toString().substring(0, 8));
            repository.save(s, venta);
            venta.setNumeroFactura(String.format("F-%06d", venta.getId()));
            return venta;
        });
    }

    /**
     * Convierte un pedido activo en una venta facturada respetando los precios congelados
     * del pedido y sin volver a descontar el stock ya reservado.
     */
    public Venta facturarPedido(Long idPedido, Long idColaborador, MetodoPago metodoPago) {
        return Tx.run("Error al facturar el pedido", s -> {
            Pedido pedido = s.createQuery(
                            "SELECT DISTINCT p FROM Pedido p JOIN FETCH p.cliente " +
                                    "LEFT JOIN FETCH p.detalles d LEFT JOIN FETCH d.producto WHERE p.id = :id", Pedido.class)
                    .setParameter("id", idPedido).uniqueResult();

            if (pedido == null) throw new NegocioException("El pedido no existe.");
            if (pedido.getEstado() == EstadoPedido.CANCELADO) throw new NegocioException("El pedido está cancelado.");
            if (pedido.getEstado() == EstadoPedido.ENTREGADO) throw new NegocioException("Este pedido ya fue facturado y entregado.");

            Colaborador colaborador = s.get(Colaborador.class, idColaborador);
            if (colaborador == null) throw new NegocioException("El colaborador no existe.");

            Venta venta = new Venta();
            venta.setCliente(pedido.getCliente());
            venta.setColaborador(colaborador);
            venta.setMetodoPago(metodoPago);
            venta.setPedido(pedido);

            for (DetallePedido dp : pedido.getDetalles()) {
                DetalleVenta dv = new DetalleVenta();
                dv.setProducto(dp.getProducto());
                dv.setCantidad(dp.getCantidad());
                dv.setPrecioUnitario(dp.getPrecioUnitario());
                venta.agregarDetalle(dv);
            }

            pedido.setEstado(EstadoPedido.ENTREGADO);
            venta.recalcularTotales();
            venta.setNumeroFactura("TMP-" + UUID.randomUUID().toString().substring(0, 8));
            repository.save(s, venta);
            venta.setNumeroFactura(String.format("F-%06d", venta.getId()));
            return venta;
        });
    }

    /** Anula la venta, devuelve el stock al inventario y cancela el pedido si estaba vinculada a uno. */
    public void anularVenta(Long idVenta) {
        Tx.exec("Error al anular la venta", s -> {
            Venta v = cargarCompleta(s, idVenta);
            if (v.getEstado() == EstadoVenta.ANULADA) throw new NegocioException("La venta ya está anulada.");

            ProductoService.devolverStock(s, cantidadesPorProducto(v));

            if (v.getPedido() != null) {
                v.getPedido().setEstado(EstadoPedido.CANCELADO);
            }
            v.setEstado(EstadoVenta.ANULADA);
        });
    }

    public List<Venta> obtenerTodas() {
        return Tx.read(s -> s.createQuery(
                "SELECT v FROM Venta v JOIN FETCH v.colaborador LEFT JOIN FETCH v.cliente ORDER BY v.id DESC",
                Venta.class).list());
    }

    /** Ideal para reportes Jasper por periodo (ambas fechas inclusive). */
    public List<Venta> obtenerPorRango(LocalDate desde, LocalDate hasta) {
        return Tx.read(s -> s.createQuery(
                        "SELECT v FROM Venta v JOIN FETCH v.colaborador LEFT JOIN FETCH v.cliente " +
                                "WHERE v.fecha >= :desde AND v.fecha < :hasta ORDER BY v.fecha", Venta.class)
                .setParameter("desde", desde.atStartOfDay())
                .setParameter("hasta", hasta.plusDays(1).atStartOfDay())
                .list());
    }

    /** Venta con líneas y productos: factura individual en Jasper. */
    public Venta obtenerCompleta(Long idVenta) {
        return Tx.read(s -> cargarCompleta(s, idVenta));
    }

    /** Filas [nombres, apellidos, totalComprado] de ventas completadas en el rango. */
    public List<Object[]> mejoresClientes(LocalDate desde, LocalDate hasta, int limite) {
        return Tx.read(s -> s.createQuery(
                        "SELECT c.nombres, c.apellidos, SUM(v.total) FROM Venta v JOIN v.cliente c " +
                                "WHERE v.estado = :estado AND v.fecha >= :desde AND v.fecha < :hasta " +
                                "GROUP BY c.id, c.nombres, c.apellidos ORDER BY SUM(v.total) DESC", Object[].class)
                .setParameter("estado", EstadoVenta.COMPLETADA)
                .setParameter("desde", desde.atStartOfDay())
                .setParameter("hasta", hasta.plusDays(1).atStartOfDay())
                .setMaxResults(limite).list());
    }

    // ---------- helpers ----------

    private Venta cargarCompleta(Session s, Long id) {
        Venta v = s.createQuery(
                        "SELECT DISTINCT v FROM Venta v JOIN FETCH v.colaborador LEFT JOIN FETCH v.cliente " +
                                "LEFT JOIN FETCH v.pedido LEFT JOIN FETCH v.detalles d LEFT JOIN FETCH d.producto " +
                                "WHERE v.id = :id", Venta.class)
                .setParameter("id", id).uniqueResult();
        if (v == null) throw new NegocioException("La venta no existe.");
        return v;
    }

    private Map<Long, Integer> cantidadesPorProducto(Venta v) {
        Map<Long, Integer> m = new TreeMap<>();
        for (DetalleVenta d : v.getDetalles()) {
            m.merge(d.getProducto().getId(), d.getCantidad(), Integer::sum);
        }
        return m;
    }
}