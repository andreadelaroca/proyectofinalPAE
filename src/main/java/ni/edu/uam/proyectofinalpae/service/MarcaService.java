package ni.edu.uam.proyectofinalpae.service;

import ni.edu.uam.proyectofinalpae.model.Marca;
import ni.edu.uam.proyectofinalpae.repository.GenericRepository;
import ni.edu.uam.proyectofinalpae.util.NegocioException;
import ni.edu.uam.proyectofinalpae.util.Tx;

import java.util.List;

public class MarcaService {
    private final GenericRepository<Marca, Long> repository = new GenericRepository<>(Marca.class);

    public Marca registrar(Marca marca) {
        return Tx.run("Error al registrar la marca", s -> {
            String nombreLimpio = validarNombre(marca.getNombre());

            Long existentes = s.createQuery(
                            "SELECT COUNT(m) FROM Marca m WHERE LOWER(m.nombre) = LOWER(:nombre)", Long.class)
                    .setParameter("nombre", nombreLimpio)
                    .uniqueResult();

            if (existentes != null && existentes > 0) {
                throw new NegocioException("La marca '" + nombreLimpio + "' ya está registrada.");
            }

            marca.setNombre(nombreLimpio);
            repository.save(s, marca);
            return marca;
        });
    }

    public Marca actualizar(Marca marca) {
        return Tx.run("Error al actualizar la marca", s -> {
            String nombreLimpio = validarNombre(marca.getNombre());

            Long existentes = s.createQuery(
                            "SELECT COUNT(m) FROM Marca m WHERE LOWER(m.nombre) = LOWER(:nombre) AND m.id <> :id", Long.class)
                    .setParameter("nombre", nombreLimpio)
                    .setParameter("id", marca.getId())
                    .uniqueResult();

            if (existentes != null && existentes > 0) {
                throw new NegocioException("Ya existe otra marca con el nombre '" + nombreLimpio + "'.");
            }

            marca.setNombre(nombreLimpio);
            repository.update(s, marca);
            return marca;
        });
    }

    public void eliminar(Marca marca) {
        Tx.exec("No se pudo eliminar la marca (puede tener productos asociados)",
                s -> repository.delete(s, marca));
    }

    public Marca buscarPorId(Long id) {
        return Tx.read(s -> repository.findById(s, id));
    }

    public boolean existeNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) return false;
        return Tx.read(s -> {
            Long count = s.createQuery(
                            "SELECT COUNT(m) FROM Marca m WHERE LOWER(m.nombre) = LOWER(:nombre)", Long.class)
                    .setParameter("nombre", nombre.trim())
                    .uniqueResult();
            return count != null && count > 0;
        });
    }

    public List<Marca> obtenerTodas() {
        return Tx.read(s -> s.createQuery(
                "SELECT m FROM Marca m ORDER BY m.id ASC", Marca.class).list());
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new NegocioException("El nombre de la marca es obligatorio.");
        }
        String limpio = nombre.trim();
        if (limpio.length() > 40) {
            throw new NegocioException("El nombre de la marca no puede superar los 40 caracteres.");
        }
        return limpio;
    }
}