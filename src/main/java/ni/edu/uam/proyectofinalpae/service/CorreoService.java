package ni.edu.uam.proyectofinalpae.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import ni.edu.uam.proyectofinalpae.util.NegocioException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Envía el código real al buzón del usuario; no imprime códigos en consola. */
public class CorreoService {
    private static String config(String nombre, String defecto) {
        String v = System.getProperty(nombre);
        if (v == null || v.isBlank()) v = System.getenv(nombre);
        return (v == null || v.isBlank()) ? defecto : v;
    }

    public void enviarCodigo(String destinatario, String codigo) {
        String host = config("SMTP_HOST", "");
        String usuario = config("SMTP_USER", "");
        String clave = config("SMTP_PASSWORD", "");
        String remitente = config("SMTP_FROM", usuario);
        String puerto = config("SMTP_PORT", "587");
        System.out.println("SMTP_HOST=" + host + " | USER vacío=" + usuario.isBlank());
        boolean ssl = Boolean.parseBoolean(config("SMTP_SSL", "false"));
        if (host.isBlank() || usuario.isBlank() || clave.isBlank() || remitente.isBlank()) {
            throw new NegocioException("SMTP sin configurar. Define SMTP_HOST, SMTP_USER, SMTP_PASSWORD y SMTP_FROM.");
        }
        Properties propiedades = new Properties();
        propiedades.put("mail.smtp.host", host);
        propiedades.put("mail.smtp.port", puerto);
        propiedades.put("mail.smtp.auth", "true");
        propiedades.put("mail.smtp.ssl.enable", Boolean.toString(ssl));
        propiedades.put("mail.smtp.starttls.enable", Boolean.toString(!ssl));
        propiedades.put("mail.smtp.starttls.required", Boolean.toString(!ssl));
        propiedades.put("mail.smtp.connectiontimeout", "10000");
        propiedades.put("mail.smtp.timeout", "10000");
        propiedades.put("mail.smtp.writetimeout", "10000");
        Session sesion = Session.getInstance(propiedades, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(usuario, clave);
            }
        });
        try {
            MimeMessage mensaje = new MimeMessage(sesion);
            mensaje.setFrom(new InternetAddress(remitente));
            mensaje.setRecipient(Message.RecipientType.TO, new InternetAddress(destinatario));
            mensaje.setSubject("Verificación de registro - BunBun Hub", StandardCharsets.UTF_8.name());
            mensaje.setText("Tu código de verificación es: " + codigo
                            + "\nExpira en 15 minutos. Si no solicitaste esta cuenta, ignora este correo.",
                    StandardCharsets.UTF_8.name());
            Transport.send(mensaje);
        } catch (MessagingException ex) {
            ex.printStackTrace();
            throw new NegocioException("Cuenta guardada, pero no se pudo enviar el código SMTP. "
                    + "Verifica configuración y usa Reenviar código.");
        }
    }
}