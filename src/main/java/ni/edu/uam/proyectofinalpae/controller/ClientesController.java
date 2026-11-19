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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalpae.model.Cliente;
import ni.edu.uam.proyectofinalpae.service.ClienteService;

import java.io.IOException;
import java.util.List;

public class ClientesController {

    @FXML private TextField txtBuscar;
    @FXML private Button btnRefrescar;
    @FXML private Button btnNuevoCliente;

    @FXML private TableView<Cliente> tblClientes;
    @FXML private TableColumn<Cliente, Long> colId;
    @FXML private TableColumn<Cliente, String> colNombres;
    @FXML private TableColumn<Cliente, String> colApellidos;
    @FXML private TableColumn<Cliente, String> colUsername;
    @FXML private TableColumn<Cliente, String> colCorreo;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colDireccion;

    private final ClienteService clienteService = new ClienteService();
    private final ObservableList<Cliente> listaClientes = FXCollections.observableArrayList();
    private FilteredList<Cliente> listaFiltrada;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarFiltro();

        btnRefrescar.setOnAction(e -> cargarClientes());
        btnNuevoCliente.setOnAction(e -> abrirNuevoCliente());

        cargarClientes();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getId()));
        colNombres.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNombres()));
        colApellidos.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getApellidos()));
        colUsername.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUsername()));
        colCorreo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCorreo()));
        colTelefono.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTelefono()));
        colDireccion.setCellValueFactory(cell -> {
            Cliente c = cell.getValue();
            String dir = (c.getDirecciones() == null || c.getDirecciones().isEmpty())
                    ? "Sin dirección"
                    : c.getDirecciones().get(0).getNombre();
            return new SimpleStringProperty(dir);
        });

        listaFiltrada = new FilteredList<>(listaClientes, p -> true);
        tblClientes.setItems(listaFiltrada);
    }

    private void configurarFiltro() {
        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> {
            String filtro = (nuevo == null) ? "" : nuevo.toLowerCase().trim();
            listaFiltrada.setPredicate(cli -> {
                if (filtro.isEmpty()) return true;
                return (cli.getNombres() != null && cli.getNombres().toLowerCase().contains(filtro))
                        || (cli.getApellidos() != null && cli.getApellidos().toLowerCase().contains(filtro))
                        || (cli.getUsername() != null && cli.getUsername().toLowerCase().contains(filtro))
                        || (cli.getCorreo() != null && cli.getCorreo().toLowerCase().contains(filtro));
            });
        });
    }

    private void cargarClientes() {
        tblClientes.setPlaceholder(new Label("Cargando clientes..."));

        Task<List<Cliente>> tarea = new Task<>() {
            @Override
            protected List<Cliente> call() {
                return clienteService.obtenerTodos();
            }
        };

        tarea.setOnSucceeded(e -> {
            listaClientes.setAll(tarea.getValue());
            tblClientes.setPlaceholder(new Label("No hay clientes registrados."));
        });

        tarea.setOnFailed(e -> {
            tblClientes.setPlaceholder(new Label("Error al cargar los clientes."));
            tarea.getException().printStackTrace();
        });

        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void abrirNuevoCliente() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ni/edu/uam/proyectofinalpae/fxml/nuevo-cliente-view.fxml")
            );
            Parent root = loader.load();
            NuevoClienteController controller = loader.getController();

            Stage modal = new Stage();
            modal.setTitle("BunBun Hub - Nuevo Cliente");
            modal.setScene(new Scene(root));
            modal.initOwner(btnNuevoCliente.getScene().getWindow());
            modal.initModality(Modality.WINDOW_MODAL);
            modal.setResizable(false);
            modal.showAndWait();

            if (controller.getClienteCreado() != null) {
                listaClientes.add(controller.getClienteCreado());
            }
        } catch (IOException e) {
            e.printStackTrace();
            Alert alerta = new Alert(Alert.AlertType.ERROR, "No se pudo abrir el formulario de cliente.");
            alerta.showAndWait();
        }
    }
}