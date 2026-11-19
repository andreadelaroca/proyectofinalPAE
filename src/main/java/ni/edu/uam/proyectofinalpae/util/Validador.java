package ni.edu.uam.proyectofinalpae.util;

import java.util.regex.Pattern;

/** Validaciones de formularios compartidas (límites iguales a los de las columnas de BD). */
public final class Validador {
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._-]{3,20}$");
    public static final int PASSWORD_MIN = 8; // debe coincidir con UsuarioService

    private Validador() {}

    /** Devuelve el mensaje de error, o null si todo está bien. Pasa los valores ya con trim(). */
    public static String validarCuenta(String nombres, String apellidos, String username, String telefono,
                                       String correo, String password, String confirmar) {
        if (nombres.isEmpty() || apellidos.isEmpty() || username.isEmpty()
                || telefono.isEmpty() || correo.isEmpty() || password.isEmpty()) {
            return "Complete todos los campos obligatorios.";
        }
        if (nombres.length() > 40 || apellidos.length() > 40) {
            return "Nombres y apellidos no pueden superar los 40 caracteres.";
        }
        if (!USERNAME.matcher(username).matches()) {
            return "El usuario debe tener de 3 a 20 caracteres (letras, números, punto, guion) y sin espacios.";
        }
        if (telefono.length() > 20) return "El teléfono no puede superar los 20 caracteres.";
        if (correo.length() > 60 || !CORREO.matcher(correo).matches()) {
            return "Ingrese un correo electrónico válido (máx. 60 caracteres).";
        }
        if (password.length() < PASSWORD_MIN) {
            return "La contraseña debe tener al menos " + PASSWORD_MIN + " caracteres.";
        }
        if (!password.equals(confirmar)) return "Las contraseñas no coinciden.";
        return null;
    }
}