package ni.edu.uam.proyectofinalpae.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalpae.model.Cliente;
import ni.edu.uam.proyectofinalpae.model.Direccion;
import ni.edu.uam.proyectofinalpae.service.ClienteService;

public class NuevoClienteController {

    @FXML private Label lblTitulo;
    @FXML private TextField txtNombres;
    @FXML private TextField txtApellidos;
    @FXML private TextField txtUsername;
    @FXML private TextField txtTelefono;
    @FXML private PasswordField pfPassword;
    @FXML private PasswordField pfConfirmarPassword;
    @FXML private TextField txtCorreo;
    @FXML private TextArea txtDireccion;
    @FXML private Button btnMapa;
    private java.math.BigDecimal latitudSeleccionada;
    private java.math.BigDecimal longitudSeleccionada;

    @FXML private Label lblMensaje;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardar;

    private final ClienteService clienteService = new ClienteService();
    private Cliente clienteCreado;
    private Cliente clienteEdicion;
    private boolean guardadoExitoso = false;

    @FXML
    public void initialize() {
        lblMensaje.setText("");
        btnCancelar.setOnAction(e -> cerrarVentana());
        btnGuardar.setOnAction(e -> guardarCliente());
        if (btnMapa != null) btnMapa.setOnAction(e -> seleccionarMapa());
    }

    public void cargarClienteParaEditar(Cliente cliente) {
        this.clienteEdicion = cliente;
        if (lblTitulo != null) {
            lblTitulo.setText("Editar Cliente");
        }
        btnGuardar.setText("Guardar Cambios");

        txtNombres.setText(cliente.getNombres());
        txtApellidos.setText(cliente.getApellidos());
        txtUsername.setText(cliente.getUsername());
        txtTelefono.setText(cliente.getTelefono());
        txtCorreo.setText(cliente.getCorreo());

        if (cliente.getDirecciones() != null && !cliente.getDirecciones().isEmpty()) {
            txtDireccion.setText(cliente.getDirecciones().get(0).getNombre());
        }

        pfPassword.setPromptText("Dejar vacío para mantener contraseña actual");
        pfConfirmarPassword.setPromptText("Repetir solo si desea cambiarla");
    }

    public Cliente getClienteCreado() {
        return clienteCreado;
    }

    public boolean isGuardadoExitoso() {
        return guardadoExitoso;
    }

    private void guardarCliente() {
        lblMensaje.setText("");

        boolean esEdicion = (clienteEdicion != null);

        String nombres = txtNombres.getText().trim();
        String apellidos = txtApellidos.getText().trim();
        String username = txtUsername.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String password = pfPassword.getText();
        String confirmar = pfConfirmarPassword.getText();
        String correo = txtCorreo.getText().trim();
        String direccionTexto = txtDireccion.getText().trim();

        if (nombres.isEmpty() || apellidos.isEmpty() || username.isEmpty()
                || telefono.isEmpty() || correo.isEmpty() || direccionTexto.isEmpty()) {
            lblMensaje.setText("Complete todos los campos obligatorios.");
            return;
        }
        if (!esEdicion && password.isEmpty()) {
            lblMensaje.setText("Debe ingresar una contraseña para el nuevo cliente.");
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
<<<<<<< HEAD
        if (password.length() < 8) {
            lblMensaje.setText("La contraseña debe tener al menos 8 caracteres.");
            return;
        }
        if (!password.equals(confirmar)) {
            lblMensaje.setText("Las contraseñas no coinciden.");
            return;
=======
        if (!password.isEmpty() || !confirmar.isEmpty()) {
            if (password.length() < 4) {
                lblMensaje.setText("La contraseña debe tener al menos 4 caracteres.");
                return;
            }
            if (!password.equals(confirmar)) {
                lblMensaje.setText("Las contraseñas no coinciden.");
                return;
            }
>>>>>>> origin/main
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
                boolean cambioUsername = !esEdicion || !username.equalsIgnoreCase(clienteEdicion.getUsername());
                if (cambioUsername && clienteService.existeUsername(username)) {
                    throw new IllegalArgumentException("El nombre de usuario '" + username + "' ya está registrado.");
                }

<<<<<<< HEAD
                Cliente nuevo = new Cliente();
                nuevo.setNombres(nombres);
                nuevo.setApellidos(apellidos);
                nuevo.setUsername(username);

                nuevo.setCorreo(correo);
                nuevo.setTelefono(telefono);

                Direccion direccion = new Direccion();
                direccion.setNombre(direccionTexto);
                direccion.setLatitud(latitudSeleccionada);
                direccion.setLongitud(longitudSeleccionada);
                nuevo.agregarDireccion(direccion);

                clienteService.registrar(nuevo, password);
                return nuevo;
=======
                Cliente target = esEdicion ? clienteEdicion : new Cliente();
                target.setNombres(nombres);
                target.setApellidos(apellidos);
                target.setUsername(username);
                target.setCorreo(correo);
                target.setTelefono(telefono);

                if (!password.isEmpty()) {
                    target.setPassword(password);
                }

                if (target.getDirecciones() != null && !target.getDirecciones().isEmpty()) {
                    target.getDirecciones().get(0).setNombre(direccionTexto);
                } else {
                    Direccion direccion = new Direccion();
                    direccion.setNombre(direccionTexto);
                    target.agregarDireccion(direccion);
                }

                if (esEdicion) {
                    clienteService.actualizar(target);
                } else {
                    clienteService.guardar(target);
                }

                return target;
>>>>>>> origin/main
            }
        };

        tareaGuardar.setOnSucceeded(e -> {
            btnGuardar.setDisable(false);
            guardadoExitoso = true;

            if (!esEdicion) {
                clienteCreado = tareaGuardar.getValue();
            }

            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
            alerta.setTitle(esEdicion ? "Cliente Actualizado" : "Cliente Registrado");
            alerta.setHeaderText(null);
<<<<<<< HEAD
            alerta.setContentText("El cliente " + clienteCreado.getNombres() + " fue registrado. Revisa el correo y confirma el código antes de iniciar sesión.");
=======
            alerta.setContentText(esEdicion
                    ? "Los datos de " + tareaGuardar.getValue().getNombres() + " fueron actualizados."
                    : "El cliente " + tareaGuardar.getValue().getNombres() + " fue registrado correctamente.");
>>>>>>> origin/main
            alerta.showAndWait();

            cerrarVentana();
        });

        tareaGuardar.setOnFailed(e -> {
            btnGuardar.setDisable(false);
            Throwable error = tareaGuardar.getException();
            lblMensaje.setText(error != null ? error.getMessage() : "Error al guardar los datos del cliente.");
        });

        Thread hilo = new Thread(tareaGuardar);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void seleccionarMapa() {
        ni.edu.uam.proyectofinalpae.map.MapaDireccionDialog.seleccionar(
                        btnMapa.getScene().getWindow(), latitudSeleccionada, longitudSeleccionada)
                .ifPresent(p -> {
                    latitudSeleccionada = p.latitud();
                    longitudSeleccionada = p.longitud();
                    btnMapa.setText("Ubicación: " + latitudSeleccionada + ", " + longitudSeleccionada);
                });
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        if (stage.getOwner() == null) {
            ni.edu.uam.proyectofinalpae.util.SceneManager.abrirVentana(
                    "/ni/edu/uam/proyectofinalpae/fxml/login.fxml", "BunBun Hub - Ingreso");
        } else {
            stage.close();
        }
    }
}