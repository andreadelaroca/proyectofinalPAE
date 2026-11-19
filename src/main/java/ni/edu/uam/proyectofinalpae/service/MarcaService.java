package ni.edu.uam.proyectofinalpae.service;

import ni.edu.uam.proyectofinalpae.model.Marca;
import ni.edu.uam.proyectofinalpae.repository.GenericRepository;
import ni.edu.uam.proyectofinalpae.util.NegocioException;
import ni.edu.uam.proyectofinalpae.util.Tx;
import org.hibernate.Session;

import java.util.List;

public class MarcaService {
    private final GenericRepository<Marca, Long> repository = new GenericRepository<>(Marca.class);

    public void guardar(Marca marca) {
        Tx.exec("Error al guardar la marca", s -> {
            validarNombreUnico(s, marca);
            repository.save(s, marca);
        });
    }

    public Marca actualizar(Marca marca) {
        return Tx.run("Error al actualizar la marca", s -> {
            validarNombreUnico(s, marca);
            repository.update(s, marca);
            return marca;
        });
    }

    public void eliminar(Marca marca) {
        Tx.exec("Error al eliminar la marca", s -> {
            Long usos = s.createQuery("SELECT COUNT(p.id) FROM Producto p WHERE p.marca.id = :id", Long.class)
                    .setParameter("id", marca.getId()).uniqueResult();
            if (usos > 0) {
                throw new NegocioException("Hay productos con esta marca; no se puede eliminar.");
            }
            repository.delete(s, marca);
        });
    }

    public Marca buscarPorId(Long id) {
        return Tx.read(s -> repository.findById(s, id));
    }

    public List<Marca> obtenerTodas() {
        return Tx.read(s -> s.createQuery("FROM Marca m ORDER BY m.nombre", Marca.class).list());
    }

    private void validarNombreUnico(Session s, Marca marca) {
        if (marca.getNombre() == null || marca.getNombre().isBlank()) {
            throw new NegocioException("El nombre de la marca es obligatorio.");
        }
        marca.setNombre(marca.getNombre().trim());
        Long id = marca.getId() == null ? -1L : marca.getId();
        Long repetidas = s.createQuery(
                        "SELECT COUNT(m.id) FROM Marca m WHERE LOWER(m.nombre) = LOWER(:n) AND m.id <> :id", Long.class)
                .setParameter("n", marca.getNombre()).setParameter("id", id).uniqueResult();
        if (repetidas > 0) throw new NegocioException("Ya existe una marca con ese nombre.");
    }
}