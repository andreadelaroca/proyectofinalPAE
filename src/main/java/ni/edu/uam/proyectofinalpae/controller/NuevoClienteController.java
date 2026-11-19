package ni.edu.uam.proyectofinalpae.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalpae.model.Cliente;
import ni.edu.uam.proyectofinalpae.model.Direccion;
import ni.edu.uam.proyectofinalpae.service.ClienteService;

public class NuevoClienteController {

    @FXML private TextField txtNombres;
    @FXML private TextField txtApellidos;
    @FXML private TextField txtUsername;
    @FXML private TextField txtTelefono;
    @FXML private PasswordField pfPassword;
    @FXML private PasswordField pfConfirmarPassword;
    @FXML private TextField txtCorreo;
    @FXML private TextArea txtDireccion;

    @FXML private Label lblMensaje;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardar;

    private final ClienteService clienteService = new ClienteService();
    private Cliente clienteCreado;

    @FXML
    public void initialize() {
        lblMensaje.setText("");
        btnCancelar.setOnAction(e -> cerrarVentana());
        btnGuardar.setOnAction(e -> guardarCliente());
    }

    public Cliente getClienteCreado() {
        return clienteCreado;
    }

    private void guardarCliente() {
        lblMensaje.setText("");

        String nombres = txtNombres.getText().trim();
        String apellidos = txtApellidos.getText().trim();
        String username = txtUsername.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String password = pfPassword.getText();
        String confirmar = pfConfirmarPassword.getText();
        String correo = txtCorreo.getText().trim();
        String direccionTexto = txtDireccion.getText().trim();

        if (nombres.isEmpty() || apellidos.isEmpty() || username.isEmpty()
                || telefono.isEmpty() || password.isEmpty() || correo.isEmpty() || direccionTexto.isEmpty()) {
            lblMensaje.setText("Complete todos los campos obligatorios.");
            return;
        }
        if (nombres.length() > 40 || apellidos.length() > 40) {
            lblMensaje.setText("Nombres y apellidos no pueden superar los 40 caracteres.");
            return;
        }
        if (username.length() > 20 || username.contains(" ")) {
            lblMensaje.setText("El usuario debe tener máximo 20 caracteres y no contener espacios.");
            return;
        }
        if (telefono.length() > 20) {
            lblMensaje.setText("El teléfono no puede superar los 20 caracteres.");
            return;
        }
        if (password.length() < 4) {
            lblMensaje.setText("La contraseña debe tener al menos 4 caracteres.");
            return;
        }
        if (!password.equals(confirmar)) {
            lblMensaje.setText("Las contraseñas no coinciden.");
            return;
        }
        if (correo.length() > 60 || !correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            lblMensaje.setText("Ingrese un correo electrónico válido (máx. 60 caracteres).");
            return;
        }
        if (direccionTexto.length() > 150) {
            lblMensaje.setText("La dirección no puede superar los 150 caracteres.");
            return;
        }

        btnGuardar.setDisable(true);

        Task<Cliente> tareaGuardar = new Task<>() {
            @Override
            protected Cliente call() {
                if (clienteService.existeUsername(username)) {
                    throw new IllegalArgumentException("El nombre de usuario '" + username + "' ya está registrado.");
                }

                Cliente nuevo = new Cliente();
                nuevo.setNombres(nombres);
                nuevo.setApellidos(apellidos);
                nuevo.setUsername(username);
                nuevo.setPassword(password);
                nuevo.setCorreo(correo);
                nuevo.setTelefono(telefono);

                Direccion direccion = new Direccion();
                direccion.setNombre(direccionTexto);
                nuevo.agregarDireccion(direccion);

                clienteService.guardar(nuevo);
                return nuevo;
            }
        };

        tareaGuardar.setOnSucceeded(e -> {
            btnGuardar.setDisable(false);
            clienteCreado = tareaGuardar.getValue();

            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
            alerta.setTitle("Cliente Registrado");
            alerta.setHeaderText(null);
            alerta.setContentText("El cliente " + clienteCreado.getNombres() + " fue registrado correctamente.");
            alerta.showAndWait();

            cerrarVentana();
        });

        tareaGuardar.setOnFailed(e -> {
            btnGuardar.setDisable(false);
            Throwable error = tareaGuardar.getException();
            lblMensaje.setText(error != null ? error.getMessage() : "Error al guardar el cliente.");
        });

        Thread hilo = new Thread(tareaGuardar);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }
}