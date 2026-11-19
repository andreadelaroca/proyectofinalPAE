package ni.edu.uam.proyectofinalpae.service;

import ni.edu.uam.proyectofinalpae.model.Direccion;
import ni.edu.uam.proyectofinalpae.repository.GenericRepository;
import ni.edu.uam.proyectofinalpae.util.NegocioException;
import ni.edu.uam.proyectofinalpae.util.Tx;

import java.util.List;

public class DireccionService {
    private final GenericRepository<Direccion, Long> repository = new GenericRepository<>(Direccion.class);

    public void guardar(Direccion direccion) {
        Tx.exec("Error al guardar la dirección", s -> repository.save(s, direccion));
    }

    public Direccion actualizar(Direccion direccion) {
        return Tx.run("Error al actualizar la dirección", s -> {
            repository.update(s, direccion);
            return direccion;
        });
    }

    public void eliminar(Direccion direccion) {
        Tx.exec("Error al eliminar la dirección", s -> {
            Long usos = s.createQuery("SELECT COUNT(p.id) FROM Pedido p WHERE p.direccionEntrega.id = :id", Long.class)
                    .setParameter("id", direccion.getId()).uniqueResult();
            if (usos > 0) {
                throw new NegocioException("La dirección está siendo usada en pedidos y no se puede eliminar.");
            }
            repository.delete(s, direccion);
        });
    }

    public Direccion buscarPorId(Long id) {
        return Tx.read(s -> repository.findById(s, id));
    }

    public List<Direccion> obtenerTodas() {
        return Tx.read(s -> repository.findAll(s));
    }

    public List<Direccion> obtenerPorCliente(Long idCliente) {
        return Tx.read(s -> s.createQuery(
                        "FROM Direccion d WHERE d.cliente.id = :id ORDER BY d.nombre", Direccion.class)
                .setParameter("id", idCliente).list());
    }
}