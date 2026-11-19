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
import java.util.regex.Pattern;


public class UsuarioService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MINUTOS_VALIDEZ_CODIGO = 15;
    private static final Pattern CORREO = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private final GenericRepository<Usuario, Long> repository = new GenericRepository<>(Usuario.class);

    public Usuario autenticar(String username, String password) {
        return Tx.read(s -> {
            Usuario u = buscarPorUsername(s, username);
            if (u == null || !PasswordUtil.verificar(password, u.getPasswordHash())) {
                throw new NegocioException("Usuario o contraseña incorrectos.");
            }
            if (!u.isActivo()) {
                throw new NegocioException("Esta cuenta está desactivada.");
            }
            if (!u.isCorreoVerificado()) {
                throw new NegocioException("Debes confirmar tu correo antes de iniciar sesión.");
            }
            return u;
        });
    }

    public void confirmarCorreo(String username, String codigo) {
        Tx.exec("Error al confirmar el correo", s -> {
            Usuario u = buscarPorUsername(s, username);
            if (u == null) throw new NegocioException("Usuario no encontrado.");
            if (u.isCorreoVerificado()) return;
            if (u.getTokenVerificacion() == null || u.getTokenExpiracion() == null
                    || LocalDateTime.now().isAfter(u.getTokenExpiracion())) {
                throw new NegocioException("El código expiró. Solicita uno nuevo.");
            }
            if (!u.getTokenVerificacion().equals(codigo == null ? "" : codigo.trim())) {
                throw new NegocioException("Código incorrecto.");
            }
            u.setCorreoVerificado(true);
            u.setTokenVerificacion(null);
            u.setTokenExpiracion(null);
        });
    }

    public String regenerarCodigo(String username) {
        return Tx.run("Error al generar el código", s -> {
            Usuario u = buscarPorUsername(s, username);
            if (u == null) throw new NegocioException("Usuario no encontrado.");
            if (u.isCorreoVerificado()) throw new NegocioException("Este correo ya fue confirmado.");
            return asignarCodigo(u);
        });
    }

    public void cambiarPassword(Long idUsuario, String actual, String nueva) {
        Tx.exec("Error al cambiar la contraseña", s -> {
            Usuario u = s.get(Usuario.class, idUsuario);
            if (u == null) throw new NegocioException("Usuario no encontrado.");
            if (!PasswordUtil.verificar(actual, u.getPasswordHash())) {
                throw new NegocioException("La contraseña actual es incorrecta.");
            }
            validarPassword(nueva);
            u.setPasswordHash(PasswordUtil.hash(nueva));
        });
    }

    public Usuario buscarPorId(Long id) {
        return Tx.read(s -> repository.findById(s, id));
    }

    public List<Usuario> obtenerTodos() {
        return Tx.read(s -> repository.findAll(s));
    }

    public boolean existeUsername(String username) {
        return Tx.read(s -> existeUsername(s, username));
    }

    public boolean existeCorreo(String correo) {
        return Tx.read(s -> existeCorreo(s, correo));
    }

    static String prepararRegistro(Session s, Usuario u, String passwordPlano) {
        if (u.getUsername() == null || u.getUsername().isBlank()) {
            throw new NegocioException("El nombre de usuario es obligatorio.");
        }
        if (u.getCorreo() == null || !CORREO.matcher(u.getCorreo().trim()).matches()) {
            throw new NegocioException("El correo no es válido.");
        }
        validarPassword(passwordPlano);

        u.setUsername(u.getUsername().trim());
        u.setCorreo(u.getCorreo().trim().toLowerCase());

        if (existeUsername(s, u.getUsername())) throw new NegocioException("El nombre de usuario ya está en uso.");
        if (existeCorreo(s, u.getCorreo())) throw new NegocioException("Ese correo ya está registrado.");

        u.setPasswordHash(PasswordUtil.hash(passwordPlano));
        u.setCorreoVerificado(false);
        u.setActivo(true);
        return asignarCodigo(u);
    }

    private static String asignarCodigo(Usuario u) {
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        u.setTokenVerificacion(codigo);
        u.setTokenExpiracion(LocalDateTime.now().plusMinutes(MINUTOS_VALIDEZ_CODIGO));
        return codigo;
    }

    private static void validarPassword(String p) {
        if (p == null || p.length() < 8) {
            throw new NegocioException("La contraseña debe tener al menos 8 caracteres.");
        }
    }

    private static Usuario buscarPorUsername(Session s, String username) {
        if (username == null) return null;
        return s.createQuery("FROM Usuario u WHERE LOWER(u.username) = LOWER(:user)", Usuario.class)
                .setParameter("user", username.trim())
                .uniqueResult();
    }

    private static boolean existeUsername(Session s, String username) {
        Long n = s.createQuery("SELECT COUNT(u.id) FROM Usuario u WHERE LOWER(u.username) = LOWER(:v)", Long.class)
                .setParameter("v", username.trim()).uniqueResult();
        return n != null && n > 0;
    }

    private static boolean existeCorreo(Session s, String correo) {
        Long n = s.createQuery("SELECT COUNT(u.id) FROM Usuario u WHERE LOWER(u.correo) = LOWER(:v)", Long.class)
                .setParameter("v", correo.trim()).uniqueResult();
        return n != null && n > 0;
    }
}