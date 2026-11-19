package ni.edu.uam.proyectofinalpae.service;

import ni.edu.uam.proyectofinalpae.model.*;
import ni.edu.uam.proyectofinalpae.repository.GenericRepository;
import ni.edu.uam.proyectofinalpae.util.NegocioException;
import ni.edu.uam.proyectofinalpae.util.Tx;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PedidoService {
    private final GenericRepository<Pedido, Long> repository = new GenericRepository<>(Pedido.class);

    public Pedido registrarPedido(Pedido pedido) {
        return Tx.run("Error al registrar el pedido", s -> {
            if (pedido.getDetalles().isEmpty()) {
                throw new NegocioException("El pedido debe tener al menos un producto.");
            }
            ProductoService.descontarStock(s, cantidadesPorProducto(pedido));
            pedido.recalcularTotal();
            repository.save(s, pedido);
            return pedido;
        });
    }

    public void cancelarPedido(Long idPedido) {
        Tx.exec("Error al cancelar el pedido", s -> {
            Pedido p = cargarConDetalles(s, idPedido);
            if (p.getEstado() == EstadoPedido.CANCELADO) throw new NegocioException("El pedido ya está cancelado.");
            if (p.getEstado() == EstadoPedido.ENTREGADO) throw new NegocioException("No se puede cancelar un pedido entregado.");
            ProductoService.devolverStock(s, cantidadesPorProducto(p));
            p.setEstado(EstadoPedido.CANCELADO);
        });
    }

    public void actualizarEstado(Long idPedido, EstadoPedido nuevoEstado) {
        if (nuevoEstado == EstadoPedido.CANCELADO) {
            cancelarPedido(idPedido);   // cancelar siempre pasa por aquí para no perder el stock
            return;
        }
        Tx.exec("Error al actualizar el estado del pedido", s -> {
            Pedido p = s.get(Pedido.class, idPedido);
            if (p == null) throw new NegocioException("El pedido no existe.");
            if (p.getEstado() == EstadoPedido.CANCELADO) throw new NegocioException("Un pedido cancelado no se puede reactivar.");
            p.setEstado(nuevoEstado);
        });
    }

    public void asignarColaborador(Long idPedido, Long idColaborador) {
        Tx.exec("Error al asignar el colaborador", s -> {
            Pedido p = s.get(Pedido.class, idPedido);
            Colaborador c = s.get(Colaborador.class, idColaborador);
            if (p == null || c == null) throw new NegocioException("Pedido o colaborador no encontrado.");
            p.setColaborador(c);
        });
    }

    public List<Pedido> obtenerTodos() {
        return Tx.read(s -> s.createQuery(
                "SELECT p FROM Pedido p JOIN FETCH p.cliente LEFT JOIN FETCH p.colaborador " +
                        "LEFT JOIN FETCH p.direccionEntrega ORDER BY p.id DESC", Pedido.class).list());
    }

    public List<Pedido> obtenerPorCliente(Long idCliente) {
        return Tx.read(s -> s.createQuery(
                        "SELECT p FROM Pedido p JOIN FETCH p.cliente LEFT JOIN FETCH p.colaborador " +
                                "LEFT JOIN FETCH p.direccionEntrega WHERE p.cliente.id = :id ORDER BY p.id DESC", Pedido.class)
                .setParameter("id", idCliente).list());
    }

    public Pedido obtenerConDetalles(Long idPedido) {
        return Tx.read(s -> cargarConDetalles(s, idPedido));
    }

    public List<Cliente> obtenerClientes() {
        return Tx.read(s -> s.createQuery("FROM Cliente c ORDER BY c.nombres", Cliente.class).list());
    }

    public List<Colaborador> obtenerColaboradores() {
        return Tx.read(s -> s.createQuery("FROM Colaborador c ORDER BY c.nombres", Colaborador.class).list());
    }

    private Pedido cargarConDetalles(org.hibernate.Session s, Long id) {
        Pedido p = s.createQuery(
                        "SELECT DISTINCT p FROM Pedido p JOIN FETCH p.cliente LEFT JOIN FETCH p.colaborador " +
                                "LEFT JOIN FETCH p.direccionEntrega LEFT JOIN FETCH p.detalles d LEFT JOIN FETCH d.producto " +
                                "WHERE p.id = :id", Pedido.class)
                .setParameter("id", id).uniqueResult();
        if (p == null) throw new NegocioException("El pedido no existe.");
        return p;
    }

    private Map<Long, Integer> cantidadesPorProducto(Pedido p) {
        Map<Long, Integer> m = new TreeMap<>();
        for (DetallePedido d : p.getDetalles()) {
            m.merge(d.getProducto().getId(), d.getCantidad(), Integer::sum);
        }
        return m;
    }
}