package ni.edu.uam.proyectofinalpae.service;

import ni.edu.uam.proyectofinalpae.model.Marca;
import ni.edu.uam.proyectofinalpae.repository.GenericRepository;
import ni.edu.uam.proyectofinalpae.util.NegocioException;
import ni.edu.uam.proyectofinalpae.util.Tx;
import org.hibernate.Session;

import java.util.List;

public class MarcaService {
    private final GenericRepository<Marca, Long> repository = new GenericRepository<>(Marca.class);

    public Marca obtenerOCrear(String nombre) {
        if (nombre == null || nombre.isBlank()) throw new NegocioException("Nombre de marca obligatorio.");
        return Tx.run("Error al buscar marca", s -> {
            Marca existente = s.createQuery("FROM Marca m WHERE LOWER(m.nombre) = LOWER(:n)", Marca.class)
                    .setParameter("n", nombre.trim()).setMaxResults(1).uniqueResult();
            if (existente != null) return existente;
            Marca nueva = new Marca(null, nombre.trim());
            repository.save(s, nueva);
            return nueva;
        });
    }

    public void guardar(Marca marca) {
        Tx.exec("Error al guardar la marca", s -> {
            validarNombreUnico(s, marca);
            repository.save(s, marca);
        });
    }

    public Marca actualizar(Marca marca) {
        return Tx.run("Error al actualizar la marca", s -> {
            validarNombreUnico(s, marca);
            return repository.update(s, marca);
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