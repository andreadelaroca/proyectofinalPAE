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
                pedido.setEstado(EstadoPedido.ENTREGADO);
                venta.setPedido(pedido);
            }

            venta.recalcularTotales();
            venta.setNumeroFactura("TMP-" + UUID.randomUUID().toString().substring(0, 8)); // NOT NULL + unique
            repository.save(s, venta);                                                     // ya tiene id
            venta.setNumeroFactura(String.format("F-%06d", venta.getId()));
            return venta;
        });
    }

    public void anularVenta(Long idVenta) {
        Tx.exec("Error al anular la venta", s -> {
            Venta v = cargarCompleta(s, idVenta);
            if (v.getEstado() == EstadoVenta.ANULADA) throw new NegocioException("La venta ya está anulada.");
            if (v.getPedido() == null) {
                ProductoService.devolverStock(s, cantidadesPorProducto(v));
            }
            v.setEstado(EstadoVenta.ANULADA);
        });
    }

    public List<Venta> obtenerTodas() {
        return Tx.read(s -> s.createQuery(
                "SELECT v FROM Venta v JOIN FETCH v.colaborador LEFT JOIN FETCH v.cliente ORDER BY v.id DESC",
                Venta.class).list());
    }

    public List<Venta> obtenerPorRango(LocalDate desde, LocalDate hasta) {
        return Tx.read(s -> s.createQuery(
                        "SELECT v FROM Venta v JOIN FETCH v.colaborador LEFT JOIN FETCH v.cliente " +
                                "WHERE v.fecha >= :desde AND v.fecha < :hasta ORDER BY v.fecha", Venta.class)
                .setParameter("desde", desde.atStartOfDay())
                .setParameter("hasta", hasta.plusDays(1).atStartOfDay())
                .list());
    }

    public Venta obtenerCompleta(Long idVenta) {
        return Tx.read(s -> cargarCompleta(s, idVenta));
    }

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