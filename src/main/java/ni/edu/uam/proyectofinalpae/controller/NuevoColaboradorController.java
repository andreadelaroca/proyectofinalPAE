package ni.edu.uam.proyectofinalpae.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalpae.model.Colaborador;
import ni.edu.uam.proyectofinalpae.service.ColaboradorService;
import ni.edu.uam.proyectofinalpae.util.NegocioException;
import ni.edu.uam.proyectofinalpae.util.PasswordUtil;

public class NuevoColaboradorController {

    @FXML private Label lblTitulo;
    @FXML private TextField txtNombres;
    @FXML private TextField txtApellidos;
    @FXML private TextField txtUsername;
    @FXML private TextField txtTelefono;
    @FXML private PasswordField pfPassword;
    @FXML private PasswordField pfConfirmarPassword;
    @FXML private TextField txtCorreo;

    @FXML private Label lblMensaje;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardar;

    private final ColaboradorService colaboradorService = new ColaboradorService();
    private Colaborador colaboradorCreado;
    private Colaborador colaboradorEdicion;
    private boolean guardadoExitoso = false;
    private String codigoVerificacionGenerado;

    @FXML
    public void initialize() {
        lblMensaje.setText("");
        btnCancelar.setOnAction(e -> cerrarVentana());
        btnGuardar.setOnAction(e -> guardarColaborador());
    }

    public void cargarColaboradorParaEditar(Colaborador colaborador) {
        this.colaboradorEdicion = colaborador;
        if (lblTitulo != null) {
            lblTitulo.setText("Editar Colaborador");
        }
        btnGuardar.setText("Guardar Cambios");

        txtNombres.setText(colaborador.getNombres());
        txtApellidos.setText(colaborador.getApellidos());
        txtUsername.setText(colaborador.getUsername());
        txtTelefono.setText(colaborador.getTelefono());
        txtCorreo.setText(colaborador.getCorreo());

        pfPassword.setPromptText("Dejar vacío para mantener contraseña actual");
        pfConfirmarPassword.setPromptText("Repetir solo si desea cambiarla");
    }

    public Colaborador getColaboradorCreado() {
        return colaboradorCreado;
    }

    public boolean isGuardadoExitoso() {
        return guardadoExitoso;
    }

    private void guardarColaborador() {
        lblMensaje.setText("");

        boolean esEdicion = (colaboradorEdicion != null);

        String nombres = txtNombres.getText().trim();
        String apellidos = txtApellidos.getText().trim();
        String username = txtUsername.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String password = pfPassword.getText();
        String confirmar = pfConfirmarPassword.getText();
        String correo = txtCorreo.getText().trim().toLowerCase();

        if (nombres.isEmpty() || apellidos.isEmpty() || username.isEmpty()
                || telefono.isEmpty() || correo.isEmpty()) {
            lblMensaje.setText("Complete todos los campos obligatorios.");
            return;
        }
        if (!esEdicion && password.isEmpty()) {
            lblMensaje.setText("Debe ingresar una contraseña para el nuevo colaborador.");
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
        if (!esEdicion) {
            if (password.length() < 8) {
                lblMensaje.setText("La contraseña debe tener al menos 8 caracteres.");
                return;
            }
        } else if (!password.isEmpty() || !confirmar.isEmpty()) {
            if (password.length() < 4) {
                lblMensaje.setText("La contraseña debe tener al menos 4 caracteres.");
                return;
            }
        }
        if (!password.equals(confirmar)) {
            lblMensaje.setText("Las contraseñas no coinciden.");
            return;
        }
        if (correo.length() > 60 || !correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            lblMensaje.setText("Ingrese un correo electrónico válido (máx. 60 caracteres).");
            return;
        }

        btnGuardar.setDisable(true);
        codigoVerificacionGenerado = null;

        Task<Colaborador> tareaGuardar = new Task<>() {
            @Override
            protected Colaborador call() {
                boolean cambioUsername = !esEdicion || !username.equalsIgnoreCase(colaboradorEdicion.getUsername());
                if (cambioUsername && colaboradorService.existeUsername(username)) {
                    throw new NegocioException("El nombre de usuario '" + username + "' ya está registrado.");
                }

                boolean cambioCorreo = !esEdicion || !correo.equalsIgnoreCase(colaboradorEdicion.getCorreo());
                if (cambioCorreo && colaboradorService.existeCorreo(correo)) {
                    throw new NegocioException("Ese correo ya está registrado.");
                }

                Colaborador target = esEdicion ? colaboradorEdicion : new Colaborador();
                target.setNombres(nombres);
                target.setApellidos(apellidos);
                target.setUsername(username);
                target.setCorreo(correo);
                target.setTelefono(telefono);

                if (esEdicion) {
                    if (!password.isEmpty()) {
                        target.setPasswordHash(PasswordUtil.hash(password));
                    }
                    return colaboradorService.actualizar(target);
                } else {
                    colaboradorService.registrar(target, password);
                }
            }
        };

        tareaGuardar.setOnSucceeded(e -> {
            btnGuardar.setDisable(false);
            guardadoExitoso = true;

            if (!esEdicion) {
                colaboradorCreado = tareaGuardar.getValue();
            }

            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
            alerta.setTitle(esEdicion ? "Colaborador Actualizado" : "Colaborador Registrado");
            alerta.setHeaderText(null);
            alerta.setContentText(esEdicion
                    ? "Los datos de " + tareaGuardar.getValue().getNombres() + " fueron actualizados."
                    : "El colaborador " + tareaGuardar.getValue().getNombres() + " fue registrado. Revisa el correo y confirma el código antes de iniciar sesión.");
            alerta.showAndWait();

            cerrarVentana();
        });

        tareaGuardar.setOnFailed(e -> {
            btnGuardar.setDisable(false);
            Throwable error = tareaGuardar.getException();
            lblMensaje.setText(error != null ? error.getMessage() : "Error al guardar los datos del colaborador.");
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