package ni.edu.uam.proyectofinalpae.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalpae.model.Colaborador;
import ni.edu.uam.proyectofinalpae.service.ColaboradorService;

public class NuevoColaboradorController {

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

    private final ColaboradorService colaboradorService = new ColaboradorService();
    private Colaborador colaboradorCreado;

    @FXML
    public void initialize() {
        lblMensaje.setText("");
        btnCancelar.setOnAction(e -> cerrarVentana());
        btnGuardar.setOnAction(e -> guardarColaborador());
    }

    public Colaborador getColaboradorCreado() {
        return colaboradorCreado;
    }

    private void guardarColaborador() {
        lblMensaje.setText("");

        String nombres = txtNombres.getText().trim();
        String apellidos = txtApellidos.getText().trim();
        String username = txtUsername.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String password = pfPassword.getText();
        String confirmar = pfConfirmarPassword.getText();
        String correo = txtCorreo.getText().trim();
        String direccion = txtDireccion.getText().trim();

        if (nombres.isEmpty() || apellidos.isEmpty() || username.isEmpty()
                || telefono.isEmpty() || password.isEmpty() || correo.isEmpty() || direccion.isEmpty()) {
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
        if (password.length() < 8) {
            lblMensaje.setText("La contraseña debe tener al menos 8 caracteres.");
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
        if (direccion.length() > 120) {
            lblMensaje.setText("La dirección no puede superar los 120 caracteres.");
            return;
        }

        btnGuardar.setDisable(true);

        Task<Colaborador> tareaGuardar = new Task<>() {
            @Override
            protected Colaborador call() {
                if (colaboradorService.existeUsername(username)) {
                    throw new IllegalArgumentException("El nombre de usuario '" + username + "' ya está registrado.");
                }

                Colaborador nuevo = new Colaborador();
                nuevo.setNombres(nombres);
                nuevo.setApellidos(apellidos);
                nuevo.setUsername(username);

                nuevo.setCorreo(correo);
                nuevo.setTelefono(telefono);
                nuevo.setDireccion(direccion);

                colaboradorService.registrar(nuevo, password);
                return nuevo;
            }
        };

        tareaGuardar.setOnSucceeded(e -> {
            btnGuardar.setDisable(false);
            colaboradorCreado = tareaGuardar.getValue();

            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
            alerta.setTitle("Colaborador Registrado");
            alerta.setHeaderText(null);
            alerta.setContentText("El colaborador " + colaboradorCreado.getNombres() + " fue registrado. Revisa el correo y confirma el código antes de iniciar sesión.");
            alerta.showAndWait();

            cerrarVentana();
        });

        tareaGuardar.setOnFailed(e -> {
            btnGuardar.setDisable(false);
            Throwable error = tareaGuardar.getException();
            lblMensaje.setText(error != null ? error.getMessage() : "Error al guardar el colaborador.");
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