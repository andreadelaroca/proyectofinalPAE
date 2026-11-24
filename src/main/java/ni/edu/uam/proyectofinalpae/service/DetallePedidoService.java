package ni.edu.uam.proyectofinalpae.service;

import ni.edu.uam.proyectofinalpae.model.DetallePedido;
import ni.edu.uam.proyectofinalpae.repository.GenericRepository;
import ni.edu.uam.proyectofinalpae.util.Tx;

import java.util.List;

/**
 * Solo lectura: modificar o borrar líneas sueltas rompería el stock reservado y el total del pedido.
 * Las líneas se gestionan mediante Pedido.agregarDetalle + PedidoService.registrarPedido.
 */
public class DetallePedidoService {
    private final GenericRepository<DetallePedido, Long> repository = new GenericRepository<>(DetallePedido.class);

    public DetallePedido buscarPorId(Long id) {
        return Tx.read(s -> repository.findById(s, id));
    }

    public List<DetallePedido> obtenerPorPedido(Long idPedido) {
        return Tx.read(s -> s.createQuery(
                        "SELECT d FROM DetallePedido d JOIN FETCH d.producto WHERE d.pedido.id = :id ORDER BY d.id",
                        DetallePedido.class)
                .setParameter("id", idPedido).list());
    }

    public List<DetallePedido> obtenerTodos() {
        return Tx.read(repository::findAll);
    }
}