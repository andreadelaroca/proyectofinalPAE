package ni.edu.uam.proyectofinalpae.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ni.edu.uam.proyectofinalpae.model.Colaborador;
import ni.edu.uam.proyectofinalpae.model.Usuario;
import ni.edu.uam.proyectofinalpae.service.UsuarioService;
import ni.edu.uam.proyectofinalpae.util.Async;
import ni.edu.uam.proyectofinalpae.util.CorreoNoVerificadoException;
import ni.edu.uam.proyectofinalpae.util.NegocioException;
import ni.edu.uam.proyectofinalpae.util.Sesion;
import ni.edu.uam.proyectofinalpae.util.Ui;
import ni.edu.uam.proyectofinalpae.util.Vistas;

import java.io.IOException;

public class LoginController {
    @FXML private TextField txtUsuario;
    @FXML private PasswordField pwfPassword;
    @FXML private Button btnIniciarSesion, btnLimpiar;
    @FXML private Hyperlink lnkRegistro;   // NUEVO: "Crear cuenta"
    @FXML private Label lblMensaje;        // NUEVO: errores bajo el formulario

    private final UsuarioService usuarioService = new UsuarioService();

    @FXML
    public void initialize() {
        lblMensaje.setText("");
        btnIniciarSesion.setDefaultButton(true);
        btnIniciarSesion.setOnAction(e -> iniciarSesion());
        btnLimpiar.setOnAction(e -> limpiar());
        lnkRegistro.setOnAction(e -> abrirRegistro());
    }

    private void iniciarSesion() {
        lblMensaje.setText("");
        String usuario = txtUsuario.getText().trim();
        String password = pwfPassword.getText();
        if (usuario.isEmpty() || password.isEmpty()) {
            lblMensaje.setText("Ingrese usuario y contraseña.");
            return;
        }

        btnIniciarSesion.setDisable(true);
        Async.ejecutar(() -> usuarioService.autenticar(usuario, password), this::alIniciar, this::alFallar);
    }

    private void alIniciar(Usuario usuario) {
        btnIniciarSesion.setDisable(false);
        pwfPassword.clear();

        if (usuario instanceof Colaborador) {
            Sesion.iniciar(usuario);
            try {
                Vistas.cambiarEscena(btnIniciarSesion.getScene().getWindow(), Vistas.MAIN_ADMIN, "BunBun Hub");
            } catch (IOException e) {
                e.printStackTrace();
                Ui.error("Error", "No se pudo abrir el panel principal.");
            }
        } else {
            // TODO: cuando exista la vista de clientes, iniciar sesión y redirigir aquí.
            Ui.info("Cuenta de cliente", "Tu cuenta está activa, pero el acceso para clientes aún no está disponible.");
        }
    }

    private void alFallar(Throwable error) {
        btnIniciarSesion.setDisable(false);
        if (error instanceof CorreoNoVerificadoException ex) {
            ofrecerVerificacion(ex);
            return;
        }
        lblMensaje.setText(error instanceof NegocioException
                ? error.getMessage()
                : "No se pudo iniciar sesión. Revise la conexión con la base de datos.");
        if (!(error instanceof NegocioException)) error.printStackTrace();
    }

    private void ofrecerVerificacion(CorreoNoVerificadoException ex) {
        boolean enviar = Ui.confirmar("Correo sin confirmar",
                ex.getMessage() + "\n\n¿Enviar un código nuevo a " + Ui.ocultarCorreo(ex.getCorreo()) + "?");
        if (!enviar) return;

        Async.ejecutar(() -> usuarioService.regenerarCodigo(ex.getUsername()),
                codigo -> VerificacionController.mostrar(btnIniciarSesion.getScene().getWindow(),
                        ex.getUsername(), ex.getCorreo(), codigo),
                error -> lblMensaje.setText(Async.mensaje(error)));
    }

    private void limpiar() {
        txtUsuario.clear();
        pwfPassword.clear();
        lblMensaje.setText("");
        txtUsuario.requestFocus();
    }

    private void abrirRegistro() {
        try {
            Vistas.cambiarEscena(lnkRegistro.getScene().getWindow(), Vistas.REGISTRO, "BunBun Hub - Registro");
        } catch (IOException e) {
            e.printStackTrace();
            Ui.error("Error", "No se pudo abrir el formulario de registro.");
        }
    }
}