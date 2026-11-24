package ni.edu.uam.proyectofinalpae.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ni.edu.uam.proyectofinalpae.util.PestanaManager;
import ni.edu.uam.proyectofinalpae.model.Marca;
import ni.edu.uam.proyectofinalpae.service.MarcaService;
import ni.edu.uam.proyectofinalpae.util.NegocioException;

public class NuevaMarcaController {

    @FXML private TextField txtNombre;
    @FXML private Label lblMensaje;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardar;

    private final MarcaService marcaService = new MarcaService();
    private Marca marcaCreada;
    private boolean guardadoExitoso = false;

    @FXML
    public void initialize() {
        lblMensaje.setText("");
        btnCancelar.setOnAction(e -> cerrarVentana());
        btnGuardar.setOnAction(e -> guardarMarca());
        txtNombre.setOnAction(e -> guardarMarca());
    }

    public Marca getMarcaCreada() {
        return marcaCreada;
    }

    public boolean isGuardadoExitoso() {
        return guardadoExitoso;
    }

    private void guardarMarca() {
        lblMensaje.setText("");
        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();

        if (nombre.isEmpty()) {
            lblMensaje.setText("Ingrese el nombre de la marca.");
            return;
        }
        if (nombre.length() > 40) {
            lblMensaje.setText("El nombre no puede superar los 40 caracteres.");
            return;
        }

        btnGuardar.setDisable(true);

        Task<Marca> tarea = new Task<>() {
            @Override
            protected Marca call() {
                if (marcaService.existeNombre(nombre)) {
                    throw new NegocioException("La marca '" + nombre + "' ya está registrada.");
                }
                Marca nueva = new Marca(null, nombre);
                return marcaService.registrar(nueva);
            }
        };

        tarea.setOnSucceeded(e -> {
            btnGuardar.setDisable(false);
            guardadoExitoso = true;
            marcaCreada = tarea.getValue();

            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
            alerta.setTitle("Marca Registrada");
            alerta.setHeaderText(null);
            alerta.setContentText("La marca '" + marcaCreada.getNombre() + "' fue agregada correctamente.");
            alerta.showAndWait();

            cerrarVentana();
        });

        tarea.setOnFailed(e -> {
            btnGuardar.setDisable(false);
            Throwable ex = tarea.getException();
            lblMensaje.setText(ex != null ? ex.getMessage() : "Error al guardar la marca.");
        });

        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void cerrarVentana() {
        PestanaManager.cerrar(btnCancelar);
    }
}