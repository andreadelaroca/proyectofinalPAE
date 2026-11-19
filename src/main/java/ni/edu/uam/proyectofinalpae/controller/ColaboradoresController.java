package ni.edu.uam.proyectofinalpae.controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ni.edu.uam.proyectofinalpae.model.Colaborador;
import ni.edu.uam.proyectofinalpae.service.ColaboradorService;
import ni.edu.uam.proyectofinalpae.util.Async;
import ni.edu.uam.proyectofinalpae.util.Ui;
import ni.edu.uam.proyectofinalpae.util.Vistas;

import java.io.IOException;

public class ColaboradoresController {

    @FXML private TextField txtBuscar;
    @FXML private Button btnRefrescar;
    @FXML private Button btnNuevoColaborador;

    @FXML private TableView<Colaborador> tblColaboradores;
    @FXML private TableColumn<Colaborador, Long> colId;
    @FXML private TableColumn<Colaborador, String> colNombres;
    @FXML private TableColumn<Colaborador, String> colApellidos;
    @FXML private TableColumn<Colaborador, String> colUsername;
    @FXML private TableColumn<Colaborador, String> colCorreo;
    @FXML private TableColumn<Colaborador, String> colTelefono;
    @FXML private TableColumn<Colaborador, String> colDireccion;

    private final ColaboradorService colaboradorService = new ColaboradorService();
    private final ObservableList<Colaborador> listaColaboradores = FXCollections.observableArrayList();
    private FilteredList<Colaborador> listaFiltrada;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarFiltro();

        btnRefrescar.setOnAction(e -> cargarColaboradores());
        btnNuevoColaborador.setOnAction(e -> abrirNuevoColaborador());

        cargarColaboradores();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getId()));
        colNombres.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNombres()));
        colApellidos.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getApellidos()));
        colUsername.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUsername()));
        colCorreo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCorreo()));
        colTelefono.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTelefono()));
        colDireccion.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDireccion()));

        listaFiltrada = new FilteredList<>(listaColaboradores, p -> true);
        tblColaboradores.setItems(listaFiltrada);
    }

    private void configurarFiltro() {
        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> {
            String filtro = (nuevo == null) ? "" : nuevo.toLowerCase().trim();
            listaFiltrada.setPredicate(col -> {
                if (filtro.isEmpty()) return true;
                return contiene(col.getNombres(), filtro) || contiene(col.getApellidos(), filtro)
                        || contiene(col.getUsername(), filtro) || contiene(col.getCorreo(), filtro);
            });
        });
    }

    private boolean contiene(String texto, String filtro) {
        return texto != null && texto.toLowerCase().contains(filtro);
    }

    private void cargarColaboradores() {
        tblColaboradores.setPlaceholder(new Label("Cargando colaboradores..."));
        Async.ejecutar(colaboradorService::obtenerTodos, lista -> {
            listaColaboradores.setAll(lista);
            tblColaboradores.setPlaceholder(new Label("No hay colaboradores registrados."));
        }, error -> {
            tblColaboradores.setPlaceholder(new Label("Error al cargar los colaboradores."));
            Async.mensaje(error);
        });
    }

    private void abrirNuevoColaborador() {
        try {
            Vistas.Vista<NuevoColaboradorController> vista = Vistas.cargar(Vistas.NUEVO_COLABORADOR);
            Vistas.mostrarModal(vista, "BunBun Hub - Nuevo Colaborador", btnNuevoColaborador.getScene().getWindow());

            Colaborador creado = vista.controller().getColaboradorCreado();
            if (creado != null) listaColaboradores.add(creado);
        } catch (IOException e) {
            e.printStackTrace();
            Ui.error("Error", "No se pudo abrir el formulario de colaborador.");
        }
    }
}