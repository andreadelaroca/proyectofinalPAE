
package ni.edu.uam.proyectofinalpae.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import ni.edu.uam.proyectofinalpae.model.Cliente;
import ni.edu.uam.proyectofinalpae.model.Colaborador;
import ni.edu.uam.proyectofinalpae.service.UsuarioService;
import ni.edu.uam.proyectofinalpae.util.SceneManager;

public class LoginController {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField pwfPassword;
    @FXML private TextField txtCodigo;
    @FXML private Label lblEstado;
    @FXML private Button btnIniciarSesion;
    @FXML private Button btnLimpiar;

    private final UsuarioService usuarios = new UsuarioService();

    @FXML
    public void initialize() {
        btnIniciarSesion.setOnAction(e -> iniciarSesion());

        btnLimpiar.setOnAction(e -> {
            txtUsuario.clear();
            pwfPassword.clear();
            txtCodigo.clear();
            lblEstado.setText("");
        });
    }

    @FXML
    public void iniciarSesion() {
        try {
            String usuario = txtUsuario.getText().trim();
            String password = pwfPassword.getText();

            if (usuario.isEmpty() || password.isEmpty()) {
                lblEstado.setText(
                        "Ingrese usuario y contraseña."
                );
                return;
            }

            var autenticado = usuarios.autenticar(
                    usuario, password
            );

            if (autenticado instanceof Colaborador) {
                SceneManager.abrirVentana(
                        "/ni/edu/uam/proyectofinalpae/fxml/main-admin.fxml",
                        "BunBun Hub - Administración"
                );
            } else {
                SceneManager.abrirVentana(
                        "/ni/edu/uam/proyectofinalpae/fxml/cliente-home-view.fxml",
                        "BunBun Hub - Cliente"
                );
            }

        } catch (RuntimeException ex) {
            lblEstado.setText(ex.getMessage());
        }
    }

    @FXML
    public void verificarCorreo() {
        try {
            String usuario = txtUsuario.getText().trim();
            String codigo = txtCodigo.getText().trim();

            if (usuario.isEmpty() || codigo.isEmpty()) {
                lblEstado.setText(
                        "Ingrese su usuario y código."
                );
                return;
            }

            usuarios.confirmarCorreo(usuario, codigo);

            lblEstado.setText(
                    "Correo verificado. Ya puede iniciar sesión."
            );
            txtCodigo.clear();

        } catch (RuntimeException ex) {
            lblEstado.setText(ex.getMessage());
        }
    }

    @FXML
    public void reenviarCodigo() {
        try {
            String usuario = txtUsuario.getText().trim();

            if (usuario.isEmpty()) {
                lblEstado.setText(
                        "Ingrese su usuario primero."
                );
                return;
            }

            usuarios.reenviarCodigo(usuario);

            lblEstado.setText(
                    "Código enviado al correo registrado."
            );

        } catch (RuntimeException ex) {
            lblEstado.setText(ex.getMessage());
        }
    }

    @FXML
    public void abrirRegistro() {
        SceneManager.abrirVentana(
                "/ni/edu/uam/proyectofinalpae/fxml/nuevo-cliente-view.fxml",
                "BunBun Hub - Registro"
        );
    }
}
