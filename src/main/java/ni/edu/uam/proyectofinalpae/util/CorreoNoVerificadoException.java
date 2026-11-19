package ni.edu.uam.proyectofinalpae.util;

/** El login fue correcto pero el correo aún no se confirmó: el controller ofrece reenviar el código. */
public class CorreoNoVerificadoException extends NegocioException {
    private final String username;
    private final String correo;

    public CorreoNoVerificadoException(String username, String correo) {
        super("Debes confirmar tu correo antes de iniciar sesión.");
        this.username = username;
        this.correo = correo;
    }

    public String getUsername() { return username; }
    public String getCorreo() { return correo; }
}