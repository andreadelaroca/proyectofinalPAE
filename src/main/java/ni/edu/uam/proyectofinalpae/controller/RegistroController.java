package ni.edu.uam.proyectofinalpae.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import ni.edu.uam.proyectofinalpae.model.Cliente;
import ni.edu.uam.proyectofinalpae.model.Direccion;
import ni.edu.uam.proyectofinalpae.service.ClienteService;
import ni.edu.uam.proyectofinalpae.util.Async;
import ni.edu.uam.proyectofinalpae.util.Ui;
import ni.edu.uam.proyectofinalpae.util.Validador;
import ni.edu.uam.proyectofinalpae.util.Vistas;

import java.io.IOException;

/** Auto-registro de clientes desde el login (registro-view.fxml). Confirma el correo con un código por SMTP. */
public class RegistroController {
    @FXML private TextField txtNombres, txtApellidos, txtUsername, txtTelefono, txtCorreo;
    @FXML private PasswordField pfPassword, pfConfirmarPassword;
    @FXML private TextArea txtDireccion;
    @FXML private Button btnUbicarMapa, btnRegistrar, btnVolver;
    @FXML private Label lblUbicacion, lblMensaje;

    private final ClienteService clienteService = new ClienteService();
    private MapaDireccionController.Ubicacion ubicacion;

    private record Resultado(Cliente cliente, String codigo) {}

    @FXML
    public void initialize() {
        lblMensaje.setText("");
        lblUbicacion.setText("Sin ubicar en el mapa (opcional).");
        btnUbicarMapa.setOnAction(e -> ubicarEnMapa());
        btnRegistrar.setOnAction(e -> registrar());
        btnVolver.setOnAction(e -> volverAlLogin());
    }

    private void ubicarEnMapa() {
        MapaDireccionController.seleccionar(btnUbicarMapa.getScene().getWindow(), txtDireccion.getText().trim())
                .ifPresent(u -> {
                    ubicacion = u;
                    lblUbicacion.setText("Ubicación marcada: " + u.latitud() + ", " + u.longitud());
                });
    }

    private void registrar() {
        lblMensaje.setText("");

        String nombres = txtNombres.getText().trim();
        String apellidos = txtApellidos.getText().trim();
        String username = txtUsername.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String correo = txtCorreo.getText().trim();
        String direccionTexto = txtDireccion.getText().trim();
        String password = pfPassword.getText();

        String error = Validador.validarCuenta(nombres, apellidos, username, telefono, correo,
                password, pfConfirmarPassword.getText());
        if (error == null && direccionTexto.isEmpty()) error = "Ingrese su dirección.";
        if (error == null && direccionTexto.length() > 150) error = "La dirección no puede superar los 150 caracteres.";
        if (error != null) {
            lblMensaje.setText(error);
            return;
        }

        btnRegistrar.setDisable(true);
        MapaDireccionController.Ubicacion ubi = ubicacion;

        Async.ejecutar(() -> {
            Cliente nuevo = new Cliente();
            nuevo.setNombres(nombres);
            nuevo.setApellidos(apellidos);
            nuevo.setUsername(username);
            nuevo.setCorreo(correo);
            nuevo.setTelefono(telefono);

            Direccion direccion = new Direccion();
            direccion.setNombre(direccionTexto);
            if (ubi != null) {
                direccion.setLatitud(ubi.latitud());
                direccion.setLongitud(ubi.longitud());
            }
            nuevo.agregarDireccion(direccion);

            String codigo = clienteService.registrar(nuevo, password);
            return new Resultado(nuevo, codigo);
        }, resultado -> {
            btnRegistrar.setDisable(false);
            boolean verificado = VerificacionController.mostrar(btnRegistrar.getScene().getWindow(),
                    username, correo, resultado.codigo());
            if (!verificado) {
                Ui.info("Cuenta creada", "Podrás confirmar tu correo más tarde al iniciar sesión.");
            }
            volverAlLogin();
        }, e -> {
            btnRegistrar.setDisable(false);
            lblMensaje.setText(Async.mensaje(e));
        });
    }

    private void volverAlLogin() {
        try {
            Vistas.cambiarEscena(btnVolver.getScene().getWindow(), Vistas.LOGIN, "BunBun Hub - Iniciar sesión");
        } catch (IOException e) {
            e.printStackTrace();
            Ui.error("Error", "No se pudo volver al inicio de sesión.");
        }
    }
}