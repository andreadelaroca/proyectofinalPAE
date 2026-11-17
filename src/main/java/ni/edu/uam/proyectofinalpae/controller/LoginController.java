package ni.edu.uam.proyectofinalpae.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ni.edu.uam.proyectofinalpae.model.Usuario;
import static ni.edu.uam.proyectofinalpae.util.Utils.*;
import static ni.edu.uam.proyectofinalpae.util.SceneManager.*;

public class LoginController {
    Usuario u = new Usuario();

    @FXML
    TextField txtUsuario;

    @FXML
    PasswordField pwPassword;

    @FXML
    Button btnIniciarSesion, btnLimpiar;

    @FXML
    public void iniciarSesion() {
        String username = txtUsuario.getText().trim();
        String password = pwPassword.getText().trim();
        if (username.isEmpty() && password.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campos incompletos", "Debe completar todos los campos para continuar.");
        } else if (username.equals(u.getUsername()) && password.equals(u.getPassword()) || username.equals("admin") && password.equals("admin")) {
            mostrarAlerta(Alert.AlertType.CONFIRMATION, "", "Inicio de sesión exitoso.");
            abrirVentana("/ni/edu/uam/proyectofinalpae/fxml/main_admin.fxml", "Dashboard");
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "Credenciales incorrectas", "Vuelva a intentarlo");
        }
    }

    @FXML
    public void limpiar() {
        txtUsuario.clear();
        pwPassword.clear();
        txtUsuario.requestFocus();
    }

    @FXML
    public void abrirRegistro() {
        abrirVentana("/ni/edu/uam/proyectofinalpae/fxml/clientes-view.fxml", "Registro");
    }
}
