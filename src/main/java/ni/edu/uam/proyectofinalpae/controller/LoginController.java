package ni.edu.uam.proyectofinalpae.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalpae.model.Colaborador;
import ni.edu.uam.proyectofinalpae.service.ColaboradorService;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField txtUsuario;

    @FXML
    private PasswordField txtContrasena;

    @FXML
    private Label lblMensaje;

    @FXML
    private Button btnIniciarSesion;

    @FXML
    private Button btnLimpiar;

    private final ColaboradorService colaboradorService = new ColaboradorService();
    private static Colaborador colaboradorActivo;

    @FXML
    public void initialize() {
        lblMensaje.setText("");
        btnIniciarSesion.setOnAction(event -> iniciarSesion());
        btnLimpiar.setOnAction(event -> limpiarCampos());
    }

    private void iniciarSesion() {
        String usuario = txtUsuario.getText().trim();
        String contrasena = txtContrasena.getText();

        if (usuario.isEmpty() || contrasena.isEmpty()) {
            lblMensaje.setText("Por favor, ingrese su usuario y contraseña.");
            return;
        }

        try {
            Colaborador colaborador = colaboradorService.autenticar(usuario, contrasena);

            if (colaborador != null) {
                colaboradorActivo = colaborador;
                abrirPanelAdmin();
            } else {
                lblMensaje.setText("Usuario o contraseña incorrectos.");
                txtContrasena.clear();
                txtContrasena.requestFocus();
            }
        } catch (Exception e) {
            lblMensaje.setText("Error de conexión con la base de datos.");
            e.printStackTrace();
        }
    }

    private void abrirPanelAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ni/edu/uam/proyectofinalpae/fxml/main_admin.fxml")
            );
            Parent root = loader.load();

            Stage stage = (Stage) btnIniciarSesion.getScene().getWindow();
            stage.setResizable(true);
            stage.setScene(new Scene(root));
            stage.setTitle("BunBun Hub - Panel Administrativo (" + colaboradorActivo.getNombres() + ")");

            // Se ejecuta en el siguiente pulso de UI para que Windows aplique bien el tamaño tras cambiar de Scene
            javafx.application.Platform.runLater(() -> {
                stage.setMaximized(false);
                stage.setMaximized(true);
            });
        } catch (IOException e) {
            lblMensaje.setText("No se pudo cargar el panel administrativo.");
            e.printStackTrace();
        }
    }

    private void limpiarCampos() {
        txtUsuario.clear();
        txtContrasena.clear();
        lblMensaje.setText("");
        txtUsuario.requestFocus();
    }

    public static Colaborador getColaboradorActivo() {
        return colaboradorActivo;
    }

    public static void cerrarSesion() {
        colaboradorActivo = null;
    }
}