package ni.edu.uam.proyectofinalpae.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalpae.model.Colaborador;
import ni.edu.uam.proyectofinalpae.service.ColaboradorService;

import java.io.IOException;
import java.util.List;

public class ColaboradoresController {

    @FXML private TextField txtBuscar;
    @FXML private Button btnRefrescar;
    @FXML private Button btnEditarColaborador;
    @FXML private Button btnNuevoColaborador;

    @FXML private TableView<Colaborador> tblColaboradores;
    @FXML private TableColumn<Colaborador, String> colNombres;
    @FXML private TableColumn<Colaborador, String> colApellidos;
    @FXML private TableColumn<Colaborador, String> colUsername;
    @FXML private TableColumn<Colaborador, String> colCorreo;
    @FXML private TableColumn<Colaborador, String> colTelefono;

    private final ColaboradorService colaboradorService = new ColaboradorService();
    private final ObservableList<Colaborador> listaColaboradores = FXCollections.observableArrayList();
    private FilteredList<Colaborador> listaFiltrada;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarFiltro();

        btnEditarColaborador.disableProperty().bind(
                tblColaboradores.getSelectionModel().selectedItemProperty().isNull()
        );

        btnRefrescar.setOnAction(e -> cargarColaboradores());
        btnEditarColaborador.setOnAction(e -> abrirModalColaborador(tblColaboradores.getSelectionModel().getSelectedItem()));
        btnNuevoColaborador.setOnAction(e -> abrirModalColaborador(null));

        cargarColaboradores();

        tblColaboradores.getColumns().forEach(col -> col.setReorderable(false));
    }

    private void configurarTabla() {
        colNombres.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNombres()));
        colApellidos.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getApellidos()));
        colUsername.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUsername()));
        colCorreo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCorreo()));
        colTelefono.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTelefono()));

        // Doble clic en una fila abre el modal en modo edición
        tblColaboradores.setRowFactory(tv -> {
            TableRow<Colaborador> fila = new TableRow<>();
            fila.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !fila.isEmpty()) {
                    abrirModalColaborador(fila.getItem());
                }
            });
            return fila;
        });

        listaFiltrada = new FilteredList<>(listaColaboradores, p -> true);
        tblColaboradores.setItems(listaFiltrada);
    }

    private void configurarFiltro() {
        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> {
            String filtro = (nuevo == null) ? "" : nuevo.toLowerCase().trim();
            listaFiltrada.setPredicate(col -> {
                if (filtro.isEmpty()) return true;
                return (col.getNombres() != null && col.getNombres().toLowerCase().contains(filtro))
                        || (col.getApellidos() != null && col.getApellidos().toLowerCase().contains(filtro))
                        || (col.getUsername() != null && col.getUsername().toLowerCase().contains(filtro))
                        || (col.getCorreo() != null && col.getCorreo().toLowerCase().contains(filtro));
            });
        });
    }

    private void cargarColaboradores() {
        tblColaboradores.setPlaceholder(new Label("Cargando colaboradores..."));
        btnRefrescar.setDisable(true);

        Task<List<Colaborador>> tarea = new Task<>() {
            @Override
            protected List<Colaborador> call() {
                return colaboradorService.obtenerTodos();
            }
        };

        tarea.setOnSucceeded(e -> {
            listaColaboradores.setAll(tarea.getValue());
            tblColaboradores.setPlaceholder(new Label("No hay colaboradores registrados."));
            btnRefrescar.setDisable(false);
        });

        tarea.setOnFailed(e -> {
            tblColaboradores.setPlaceholder(new Label("Error al cargar los colaboradores."));
            btnRefrescar.setDisable(false);
            tarea.getException().printStackTrace();
        });

        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void abrirModalColaborador(Colaborador colaboradorAEditar) {
        btnNuevoColaborador.setDisable(true);
        btnEditarColaborador.disableProperty().unbind();
        btnEditarColaborador.setDisable(true);
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ni/edu/uam/proyectofinalpae/fxml/nuevo-colaborador-view.fxml")
            );
            Parent root = loader.load();
            NuevoColaboradorController controller = loader.getController();

            if (colaboradorAEditar != null) {
                controller.cargarColaboradorParaEditar(colaboradorAEditar);
            }

            Stage modal = new Stage();
            modal.setTitle(colaboradorAEditar == null ? "BunBun Hub - Nuevo Colaborador" : "BunBun Hub - Editar Colaborador");
            modal.setScene(new Scene(root));
            modal.initOwner(btnNuevoColaborador.getScene().getWindow());
            modal.initModality(Modality.WINDOW_MODAL);
            modal.setResizable(false);
            modal.showAndWait();

            if (colaboradorAEditar == null && controller.getColaboradorCreado() != null) {
                listaColaboradores.add(controller.getColaboradorCreado());
            } else if (colaboradorAEditar != null && controller.isGuardadoExitoso()) {
                tblColaboradores.refresh();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Alert alerta = new Alert(Alert.AlertType.ERROR, "No se pudo abrir el formulario de colaborador.");
            alerta.showAndWait();
        } finally {
            btnNuevoColaborador.setDisable(false);
            btnEditarColaborador.disableProperty().bind(
                    tblColaboradores.getSelectionModel().selectedItemProperty().isNull()
            );
        }
    }
}