package ni.edu.uam.proyectofinalpae.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import ni.edu.uam.proyectofinalpae.util.NegocioException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Envío de correos por SMTP. Credenciales en src/main/resources/smtp.properties (NO subir a Git):
 *   host=smtp.gmail.com
 *   port=587
 *   user=tu_cuenta@gmail.com
 *   password=contraseña_de_aplicacion
 *   from=BunBun Hub <tu_cuenta@gmail.com>
 * En Gmail hay que usar una "contraseña de aplicación" (requiere verificación en 2 pasos).
 */
public class CorreoService {
    private static final String ARCHIVO_CONFIG = "/smtp.properties";

    /** Bloquea el hilo: llamar siempre desde Async. */
    public void enviarCodigoVerificacion(String destino, String nombre, String codigo) {
        String asunto = "BunBun Hub - Código de verificación";
        String cuerpo = "Hola " + nombre + ",\n\n"
                + "Tu código para confirmar tu correo es: " + codigo + "\n\n"
                + "El código vence en 15 minutos. Si no solicitaste esta cuenta, ignora este mensaje.\n\n"
                + "BunBun Hub";
        enviar(destino, asunto, cuerpo);
    }

    public void enviar(String destino, String asunto, String cuerpo) {
        Properties cfg = cargarConfiguracion();
        String usuario = cfg.getProperty("user");
        String clave = cfg.getProperty("password");

        Properties props = new Properties();
        props.put("mail.smtp.host", cfg.getProperty("host", "smtp.gmail.com"));
        props.put("mail.smtp.port", cfg.getProperty("port", "587"));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        Session sesion = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(usuario, clave);
            }
        });

        try {
            MimeMessage mensaje = new MimeMessage(sesion);
            mensaje.setFrom(new InternetAddress(cfg.getProperty("from", usuario)));
            mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destino));
            mensaje.setSubject(asunto, "UTF-8");
            mensaje.setText(cuerpo, "UTF-8");
            Transport.send(mensaje);
        } catch (Exception e) {
            throw new NegocioException("No se pudo enviar el correo. Revise la configuración SMTP y la conexión.");
        }
    }

    private Properties cargarConfiguracion() {
        try (InputStream in = CorreoService.class.getResourceAsStream(ARCHIVO_CONFIG)) {
            if (in == null) throw new NegocioException("Falta el archivo smtp.properties en resources.");
            Properties p = new Properties();
            p.load(in);
            if (p.getProperty("user") == null || p.getProperty("password") == null) {
                throw new NegocioException("smtp.properties debe definir 'user' y 'password'.");
            }
            return p;
        } catch (IOException e) {
            throw new NegocioException("No se pudo leer smtp.properties.");
        }
    }
}