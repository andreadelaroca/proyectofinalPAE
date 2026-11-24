package ni.edu.uam.proyectofinalpae.service;

import ni.edu.uam.proyectofinalpae.model.Cliente;
import ni.edu.uam.proyectofinalpae.repository.GenericRepository;
import ni.edu.uam.proyectofinalpae.util.Tx;

import java.util.List;

public class ClienteService {
    private final GenericRepository<Cliente, Long> repository = new GenericRepository<>(Cliente.class);
    private final UsuarioService usuarioService = new UsuarioService();

    public String registrar(Cliente cliente, String passwordPlano) {
        return Tx.run("Error al registrar el cliente", s -> {
            String codigo = UsuarioService.prepararRegistro(s, cliente, passwordPlano);
            repository.save(s, cliente);
            return codigo;
        });
    }

    public Cliente actualizar(Cliente cliente) {
        return Tx.run("Error al actualizar el cliente", s -> {
            repository.update(s, cliente);
            return cliente;
        });
    }

    public Cliente buscarPorId(Long id) {
        return Tx.read(s -> repository.findById(s, id));
    }

    /** Cliente con sus direcciones ya cargadas (para usarlo fuera de la sesión de Hibernate). */
    public Cliente buscarConDirecciones(Long id) {
        return Tx.read(s -> s.createQuery(
                        "SELECT DISTINCT c FROM Cliente c LEFT JOIN FETCH c.direcciones WHERE c.id = :id",
                        Cliente.class)
                .setParameter("id", id)
                .uniqueResult());
    }

    public List<Cliente> obtenerTodos() {
        return Tx.read(s -> s.createQuery(
                "SELECT DISTINCT c FROM Cliente c LEFT JOIN FETCH c.direcciones ORDER BY c.id ASC",
                Cliente.class).list());
    }

    public boolean existeUsername(String username) {
        return usuarioService.existeUsername(username);
    }

    public boolean existeCorreo(String correo) {
        return usuarioService.existeCorreo(correo);
    }
}