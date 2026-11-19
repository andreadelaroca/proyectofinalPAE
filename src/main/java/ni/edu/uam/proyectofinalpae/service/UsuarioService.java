package ni.edu.uam.proyectofinalpae.service;

import ni.edu.uam.proyectofinalpae.model.Usuario;
import ni.edu.uam.proyectofinalpae.repository.GenericRepository;
import ni.edu.uam.proyectofinalpae.util.NegocioException;
import ni.edu.uam.proyectofinalpae.util.PasswordUtil;
import ni.edu.uam.proyectofinalpae.util.Tx;
import org.hibernate.Session;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class UsuarioService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int VALIDEZ_MINUTOS = 15;
    private static final Pattern CORREO = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private final GenericRepository<Usuario, Long> repository = new GenericRepository<>(Usuario.class);
    private final CorreoService correoService = new CorreoService();

    public Usuario autenticar(String username, String password) {
        return Tx.read(s -> {
            Usuario u = buscarPorUsername(s, username);
            if (u == null || !PasswordUtil.verificar(password, u.getPasswordHash()))
                throw new NegocioException("Usuario o contraseña incorrectos.");
            if (!u.isActivo()) throw new NegocioException("La cuenta está desactivada.");
            if (!u.isCorreoVerificado()) throw new NegocioException("Confirma tu correo antes de ingresar.");
            return u;
        });
    }

    public void confirmarCorreo(String username, String codigo) {
        Tx.exec("Error al confirmar el correo", s -> {
            Usuario u = buscarPorUsername(s, username);
            if (u == null) throw new NegocioException("Usuario no encontrado.");
            if (u.isCorreoVerificado()) return;
            if (u.getTokenVerificacion() == null || u.getTokenExpiracion() == null ||
                    !LocalDateTime.now().isBefore(u.getTokenExpiracion()))
                throw new NegocioException("El código expiró. Solicita otro.");
            if (!u.getTokenVerificacion().equals(codigo == null ? "" : codigo.trim()))
                throw new NegocioException("Código incorrecto.");
            u.setCorreoVerificado(true);
            u.setTokenVerificacion(null);
            u.setTokenExpiracion(null);
        });
    }

    /** Guarda un código nuevo y lo envía al correo asociado. */
    public void reenviarCodigo(String username) {
        DatosCorreo datos = Tx.run("Error al regenerar el código", s -> {
            Usuario u = buscarPorUsername(s, username);
            if (u == null) throw new NegocioException("Usuario no encontrado.");
            if (u.isCorreoVerificado()) throw new NegocioException("Correo ya verificado.");
            return new DatosCorreo(u.getCorreo(), asignarCodigo(u));
        });
        correoService.enviarCodigo(datos.destino(), datos.codigo());
    }

    public void cambiarPassword(Long idUsuario, String actual, String nueva) {
        Tx.exec("Error al cambiar contraseña", s -> {
            Usuario u = s.get(Usuario.class, idUsuario);
            if (u == null) throw new NegocioException("Usuario no encontrado.");
            if (!PasswordUtil.verificar(actual, u.getPasswordHash()))
                throw new NegocioException("Contraseña actual incorrecta.");
            validarPassword(nueva);
            u.setPasswordHash(PasswordUtil.hash(nueva));
        });
    }

    public Usuario buscarPorId(Long id) { return Tx.read(s -> repository.findById(s, id)); }
    public List<Usuario> obtenerTodos() { return Tx.read(s -> repository.findAll(s)); }
    public boolean existeUsername(String nombre) { return Tx.read(s -> existe(s, "username", nombre)); }
    public boolean existeCorreo(String correo) { return Tx.read(s -> existe(s, "correo", correo)); }

    static String prepararRegistro(Session s, Usuario u, String passwordPlano) {
        if (u.getUsername() == null || !u.getUsername().trim().matches("[A-Za-z0-9_.-]{3,20}"))
            throw new NegocioException("Usuario inválido (3 a 20 caracteres, sin espacios).");
        if (u.getCorreo() == null || !CORREO.matcher(u.getCorreo().trim()).matches() ||
                u.getCorreo().length() > 60)
            throw new NegocioException("Correo inválido.");
        validarPassword(passwordPlano);
        u.setUsername(u.getUsername().trim().toLowerCase(Locale.ROOT));
        u.setCorreo(u.getCorreo().trim().toLowerCase(Locale.ROOT));
        if (existe(s, "username", u.getUsername())) throw new NegocioException("Usuario ocupado.");
        if (existe(s, "correo", u.getCorreo())) throw new NegocioException("Correo ya registrado.");
        u.setPasswordHash(PasswordUtil.hash(passwordPlano));
        u.setCorreoVerificado(false);
        u.setActivo(true);
        return asignarCodigo(u);
    }

    private static void validarPassword(String p) {
        if (p == null || p.length() < 8) throw new NegocioException("Contraseña de al menos 8 caracteres.");
    }

    private static String asignarCodigo(Usuario u) {
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        u.setTokenVerificacion(codigo);
        u.setTokenExpiracion(LocalDateTime.now().plusMinutes(VALIDEZ_MINUTOS));
        return codigo;
    }

    private static Usuario buscarPorUsername(Session s, String username) {
        if (username == null || username.isBlank()) return null;
        return s.createQuery("FROM Usuario u WHERE LOWER(u.username) = :nombre", Usuario.class)
                .setParameter("nombre", username.trim().toLowerCase(Locale.ROOT)).uniqueResult();
    }
    private static boolean existe(Session s, String columna, String valor) {
        if (valor == null || valor.isBlank()) return false;
        String hql = columna.equals("username") ? "u.username" : "u.correo";
        Long cuenta = s.createQuery("SELECT COUNT(u.id) FROM Usuario u WHERE LOWER(" + hql + ") = :v", Long.class)
                .setParameter("v", valor.trim().toLowerCase(Locale.ROOT)).uniqueResult();
        return cuenta != null && cuenta > 0;
    }
    private record DatosCorreo(String destino, String codigo) {}
}