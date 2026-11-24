package ni.edu.uam.proyectofinalpae.controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import javafx.scene.control.*;

import ni.edu.uam.proyectofinalpae.util.PestanaManager;
import ni.edu.uam.proyectofinalpae.model.Marca;
import ni.edu.uam.proyectofinalpae.service.MarcaService;

import java.util.List;

public class GestionMarcasController {

    @FXML private TextField txtBuscar;
    @FXML private Button btnRefrescar;
    @FXML private Button btnNuevaMarca;

    @FXML private TableView<Marca> tblMarcas;
    @FXML private TableColumn<Marca, Long> colId;
    @FXML private TableColumn<Marca, String> colNombre;

    private final MarcaService marcaService = new MarcaService();
    private final ObservableList<Marca> listaMarcas = FXCollections.observableArrayList();
    private FilteredList<Marca> listaFiltrada;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getId()));
        colNombre.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNombre()));

        listaFiltrada = new FilteredList<>(listaMarcas, m -> true);
        tblMarcas.setItems(listaFiltrada);

        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> filtrar(newVal));
        btnRefrescar.setOnAction(e -> cargarMarcas());
        btnNuevaMarca.setOnAction(e -> abrirModalNuevaMarca());

        cargarMarcas();
    }

    private void filtrar(String texto) {
        String filtro = texto == null ? "" : texto.trim().toLowerCase();
        listaFiltrada.setPredicate(marca -> {
            if (filtro.isEmpty()) return true;
            boolean coincideId = marca.getId() != null && marca.getId().toString().contains(filtro);
            boolean coincideNombre = marca.getNombre() != null && marca.getNombre().toLowerCase().contains(filtro);
            return coincideId || coincideNombre;
        });
    }

    private void cargarMarcas() {
        btnRefrescar.setDisable(true);

        Task<List<Marca>> tarea = new Task<>() {
            @Override
            protected List<Marca> call() {
                return marcaService.obtenerTodas();
            }
        };

        tarea.setOnSucceeded(e -> {
            btnRefrescar.setDisable(false);
            listaMarcas.setAll(tarea.getValue());
            filtrar(txtBuscar.getText());
        });

        tarea.setOnFailed(e -> {
            btnRefrescar.setDisable(false);
            Throwable ex = tarea.getException();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(ex != null ? ex.getMessage() : "No se pudieron cargar las marcas.");
            alert.showAndWait();
        });

        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void abrirModalNuevaMarca() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ni/edu/uam/proyectofinalpae/fxml/nueva-marca.fxml"));
            Parent root = loader.load();

            NuevaMarcaController controller = loader.getController();

            btnNuevaMarca.setDisable(true);
            PestanaManager.abrir(root, "Nueva marca", btnNuevaMarca, () -> {
                btnNuevaMarca.setDisable(false);
                if (controller.isGuardadoExitoso()) {
                    cargarMarcas();
                }
            });
        } catch (Exception ex) {
            btnNuevaMarca.setDisable(false);
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("No se pudo abrir la ventana de nueva marca: " + ex.getMessage());
            alert.showAndWait();
        }
    }
}