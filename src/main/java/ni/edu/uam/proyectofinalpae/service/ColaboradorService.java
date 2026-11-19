package ni.edu.uam.proyectofinalpae.service;

import ni.edu.uam.proyectofinalpae.model.Colaborador;
import ni.edu.uam.proyectofinalpae.repository.GenericRepository;
import ni.edu.uam.proyectofinalpae.util.NegocioException;
import ni.edu.uam.proyectofinalpae.util.Tx;

import java.util.List;

public class ColaboradorService {
    private final GenericRepository<Colaborador, Long> repository = new GenericRepository<>(Colaborador.class);
    private final UsuarioService usuarioService = new UsuarioService();

    public String registrar(Colaborador colaborador, String passwordPlano) {
        return Tx.run("Error al registrar el colaborador", s -> {
            String codigo = UsuarioService.prepararRegistro(s, colaborador, passwordPlano);
            repository.save(s, colaborador);
            return codigo;
        });
    }

    public Colaborador actualizar(Colaborador colaborador) {
        return Tx.run("Error al actualizar el colaborador", s -> repository.update(s, colaborador));
    }

    public void eliminar(Colaborador colaborador) {
        Tx.exec("Error al eliminar el colaborador", s -> {
            Long pedidos = s.createQuery("SELECT COUNT(p.id) FROM Pedido p WHERE p.colaborador.id = :id", Long.class)
                    .setParameter("id", colaborador.getId()).uniqueResult();
            Long ventas = s.createQuery("SELECT COUNT(v.id) FROM Venta v WHERE v.colaborador.id = :id", Long.class)
                    .setParameter("id", colaborador.getId()).uniqueResult();
            if (pedidos > 0 || ventas > 0) {
                throw new NegocioException("El colaborador tiene pedidos o ventas asociados. Desactívalo en lugar de eliminarlo.");
            }
            repository.delete(s, colaborador);
        });
    }

    public Colaborador buscarPorId(Long id) {
        return Tx.read(s -> repository.findById(s, id));
    }

    public List<Colaborador> obtenerTodos() {
        return Tx.read(s -> repository.findAll(s));
    }

    public boolean existeUsername(String username) {
        return usuarioService.existeUsername(username);
    }

    public boolean existeCorreo(String correo) {
        return usuarioService.existeCorreo(correo);
    }
}