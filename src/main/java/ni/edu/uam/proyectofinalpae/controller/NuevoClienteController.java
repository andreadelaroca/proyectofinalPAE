package ni.edu.uam.proyectofinalpae.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalpae.model.Cliente;
import ni.edu.uam.proyectofinalpae.model.Direccion;
import ni.edu.uam.proyectofinalpae.service.ClienteService;
import ni.edu.uam.proyectofinalpae.util.Async;
import ni.edu.uam.proyectofinalpae.util.Validador;

public class NuevoClienteController {

    @FXML private TextField txtNombres;
    @FXML private TextField txtApellidos;
    @FXML private TextField txtUsername;
    @FXML private TextField txtTelefono;
    @FXML private PasswordField pfPassword;
    @FXML private PasswordField pfConfirmarPassword;
    @FXML private TextField txtCorreo;
    @FXML private TextArea txtDireccion;
    @FXML private Button btnUbicarMapa;   // NUEVO: marcar la dirección en OpenStreetMap (opcional)
    @FXML private Label lblUbicacion;     // NUEVO

    @FXML private Label lblMensaje;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardar;

    private final ClienteService clienteService = new ClienteService();
    private Cliente clienteCreado;
    private MapaDireccionController.Ubicacion ubicacion;

    private record Resultado(Cliente cliente, String codigo) {}

    @FXML
    public void initialize() {
        lblMensaje.setText("");
        lblUbicacion.setText("Sin ubicar en el mapa (opcional).");
        btnCancelar.setOnAction(e -> cerrarVentana());
        btnGuardar.setOnAction(e -> guardarCliente());
        btnUbicarMapa.setOnAction(e -> ubicarEnMapa());
    }

    public Cliente getClienteCreado() {
        return clienteCreado;
    }

    private void ubicarEnMapa() {
        MapaDireccionController.seleccionar(btnUbicarMapa.getScene().getWindow(), txtDireccion.getText().trim())
                .ifPresent(u -> {
                    ubicacion = u;
                    lblUbicacion.setText("Ubicación marcada: " + u.latitud() + ", " + u.longitud());
                });
    }

    private void guardarCliente() {
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
        if (error == null && direccionTexto.isEmpty()) error = "Ingrese la dirección del cliente.";
        if (error == null && direccionTexto.length() > 150) error = "La dirección no puede superar los 150 caracteres.";
        if (error != null) {
            lblMensaje.setText(error);
            return;
        }

        btnGuardar.setDisable(true);
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

            String codigo = clienteService.registrar(nuevo, password); // hashea la contraseña y genera el código
            return new Resultado(nuevo, codigo);
        }, resultado -> {
            btnGuardar.setDisable(false);
            clienteCreado = resultado.cliente();
            // Envía el código por SMTP y pide confirmarlo; si lo cierra, queda pendiente de verificar.
            VerificacionController.mostrar(btnGuardar.getScene().getWindow(), username, correo, resultado.codigo());
            cerrarVentana();
        }, e -> {
            btnGuardar.setDisable(false);
            lblMensaje.setText(Async.mensaje(e));
        });
    }

    private void cerrarVentana() {
        ((Stage) btnCancelar.getScene().getWindow()).close();
    }
}