package ni.edu.uam.proyectofinalpae.controller;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import ni.edu.uam.proyectofinalpae.service.CorreoService;
import ni.edu.uam.proyectofinalpae.service.UsuarioService;
import ni.edu.uam.proyectofinalpae.util.Async;
import ni.edu.uam.proyectofinalpae.util.Ui;
import ni.edu.uam.proyectofinalpae.util.Vistas;

import java.io.IOException;

/** Modal "confirmar correo" (verificacion-view.fxml). Envía el código por SMTP al abrirse. */
public class VerificacionController {
    @FXML private Label lblCorreo, lblMensaje;
    @FXML private TextField txtCodigo;
    @FXML private Button btnVerificar, btnReenviar, btnCerrar;

    private final UsuarioService usuarioService = new UsuarioService();
    private final CorreoService correoService = new CorreoService();
    private String username;
    private String correo;
    private boolean verificado;

    /** Abre el modal, espera a que se cierre y devuelve true si el correo quedó confirmado. */
    public static boolean mostrar(Window owner, String username, String correo, String codigo) {
        try {
            Vistas.Vista<VerificacionController> vista = Vistas.cargar(Vistas.VERIFICACION);
            vista.controller().configurar(username, correo, codigo);
            Vistas.mostrarModal(vista, "BunBun Hub - Confirmar correo", owner);
            return vista.controller().verificado;
        } catch (IOException e) {
            e.printStackTrace();
            Ui.error("Error", "No se pudo abrir la ventana de verificación.");
            return false;
        }
    }

    @FXML
    public void initialize() {
        lblMensaje.setText("");
        btnVerificar.setOnAction(e -> verificar());
        btnReenviar.setOnAction(e -> reenviar());
        btnCerrar.setOnAction(e -> cerrar());
    }

    private void configurar(String username, String correo, String codigoInicial) {
        this.username = username;
        this.correo = correo;
        lblCorreo.setText("Enviamos un código de 6 dígitos a " + Ui.ocultarCorreo(correo));
        enviar(codigoInicial);
    }

    private void enviar(String codigo) {
        lblMensaje.setText("Enviando código...");
        Async.ejecutarSinResultado(
                () -> correoService.enviarCodigoVerificacion(correo, username, codigo),
                () -> lblMensaje.setText("Código enviado. Revisa tu bandeja de entrada (y el spam)."),
                e -> lblMensaje.setText(Async.mensaje(e) + " Usa «Reenviar código» para intentar de nuevo."));
    }

    private void verificar() {
        String codigo = txtCodigo.getText().trim();
        if (codigo.isEmpty()) {
            lblMensaje.setText("Ingrese el código que recibió por correo.");
            return;
        }
        btnVerificar.setDisable(true);
        Async.ejecutarSinResultado(
                () -> usuarioService.confirmarCorreo(username, codigo),
                () -> {
                    verificado = true;
                    Ui.info("Correo confirmado", "Tu correo fue confirmado. Ya puedes iniciar sesión.");
                    cerrar();
                },
                e -> {
                    btnVerificar.setDisable(false);
                    lblMensaje.setText(Async.mensaje(e));
                });
    }

    private void reenviar() {
        btnReenviar.setDisable(true);
        Async.ejecutar(() -> usuarioService.regenerarCodigo(username), this::enviar,
                e -> lblMensaje.setText(Async.mensaje(e)));

        // Evita spam al servidor SMTP: espera 30 s antes de permitir otro reenvío.
        PauseTransition espera = new PauseTransition(Duration.seconds(30));
        espera.setOnFinished(e -> btnReenviar.setDisable(false));
        espera.play();
    }

    private void cerrar() {
        ((Stage) btnCerrar.getScene().getWindow()).close();
    }
}