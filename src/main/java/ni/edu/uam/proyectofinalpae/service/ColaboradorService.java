package ni.edu.uam.proyectofinalpae.service;

import ni.edu.uam.proyectofinalpae.model.Colaborador;
import ni.edu.uam.proyectofinalpae.repository.GenericRepository;
import ni.edu.uam.proyectofinalpae.util.NegocioException;
import ni.edu.uam.proyectofinalpae.util.Tx;
import java.util.List;

public class ColaboradorService {
    private final GenericRepository<Colaborador, Long> repo = new GenericRepository<>(Colaborador.class);
    private final UsuarioService usuarios = new UsuarioService();
    private final CorreoService correo = new CorreoService();
    public Colaborador registrar(Colaborador colaborador, String contrasena) {
        String codigo = Tx.run("Error al registrar colaborador", s -> {
            String c = UsuarioService.prepararRegistro(s, colaborador, contrasena);
            repo.save(s, colaborador);
            return c;
        });
        correo.enviarCodigo(colaborador.getCorreo(), codigo);
        return colaborador;
    }
    public Colaborador actualizar(Colaborador c) { return Tx.run("Error al actualizar colaborador", s -> repo.update(s, c)); }
    public void eliminar(Colaborador c) {
        Tx.exec("Error al eliminar colaborador", s -> {
            Long p = s.createQuery("SELECT COUNT(p.id) FROM Pedido p WHERE p.colaborador.id = :id", Long.class)
                    .setParameter("id", c.getId()).uniqueResult();
            Long v = s.createQuery("SELECT COUNT(v.id) FROM Venta v WHERE v.colaborador.id = :id", Long.class)
                    .setParameter("id", c.getId()).uniqueResult();
            if (p > 0 || v > 0) throw new NegocioException("Tiene pedidos o ventas; desactívalo.");
            repo.delete(s, c);
        });
    }
    public Colaborador buscarPorId(Long id) { return Tx.read(s -> repo.findById(s, id)); }
    public List<Colaborador> obtenerTodos() { return Tx.read(s -> repo.findAll(s)); }
    public boolean existeUsername(String u) { return usuarios.existeUsername(u); }
    public boolean existeCorreo(String e) { return usuarios.existeCorreo(e); }
}