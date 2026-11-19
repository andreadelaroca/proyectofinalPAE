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
import ni.edu.uam.proyectofinalpae.model.Cliente;
import ni.edu.uam.proyectofinalpae.service.ClienteService;

import java.io.IOException;
import java.util.List;

public class ClientesController {

    @FXML private TextField txtBuscar;
    @FXML private Button btnRefrescar;
    @FXML private Button btnEditarCliente;
    @FXML private Button btnNuevoCliente;
    @FXML private Button btnVerMapa;

    @FXML private TableView<Cliente> tblClientes;
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

        btnEditarCliente.disableProperty().bind(
                tblClientes.getSelectionModel().selectedItemProperty().isNull()
        );

        btnRefrescar.setOnAction(e -> cargarClientes());
        btnEditarCliente.setOnAction(e -> abrirModalCliente(tblClientes.getSelectionModel().getSelectedItem()));
        btnNuevoCliente.setOnAction(e -> abrirModalCliente(null));
        cargarClientes();

        tblClientes.getColumns().forEach(col -> col.setReorderable(false));
    }

    private void configurarTabla() {
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

        // Doble clic en una fila abre el modal en modo edición
        tblClientes.setRowFactory(tv -> {
            TableRow<Cliente> fila = new TableRow<>();
            fila.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !fila.isEmpty()) {
                    abrirModalCliente(fila.getItem());
                }
            });
            return fila;
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
        btnRefrescar.setDisable(true);

        Task<List<Cliente>> tarea = new Task<>() {
            @Override
            protected List<Cliente> call() {
                return clienteService.obtenerTodos();
            }
        };

        tarea.setOnSucceeded(e -> {
            listaClientes.setAll(tarea.getValue());
            tblClientes.setPlaceholder(new Label("No hay clientes registrados."));
            btnRefrescar.setDisable(false);
        });

        tarea.setOnFailed(e -> {
            tblClientes.setPlaceholder(new Label("Error al cargar los clientes."));
            btnRefrescar.setDisable(false);
            tarea.getException().printStackTrace();
        });

        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void abrirModalCliente(Cliente clienteAEditar) {
        btnNuevoCliente.setDisable(true);
        btnEditarCliente.disableProperty().unbind();
        btnEditarCliente.setDisable(true);
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ni/edu/uam/proyectofinalpae/fxml/nuevo-cliente-view.fxml")
            );
            Parent root = loader.load();
            NuevoClienteController controller = loader.getController();

            if (clienteAEditar != null) {
                controller.cargarClienteParaEditar(clienteAEditar);
            }

            Stage modal = new Stage();
            modal.setTitle(clienteAEditar == null ? "BunBun Hub - Nuevo Cliente" : "BunBun Hub - Editar Cliente");
            modal.setScene(new Scene(root));
            modal.initOwner(btnNuevoCliente.getScene().getWindow());
            modal.initModality(Modality.WINDOW_MODAL);
            modal.setResizable(false);
            modal.showAndWait();

            if (clienteAEditar == null && controller.getClienteCreado() != null) {
                listaClientes.add(controller.getClienteCreado());
            } else if (clienteAEditar != null && controller.isGuardadoExitoso()) {
                tblClientes.refresh();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Alert alerta = new Alert(Alert.AlertType.ERROR, "No se pudo abrir el formulario de cliente.");
            alerta.showAndWait();
        } finally {
            btnNuevoCliente.setDisable(false);
            btnEditarCliente.disableProperty().bind(
                    tblClientes.getSelectionModel().selectedItemProperty().isNull()
            );
        }
    }
    private void verEnMapa() {
        Cliente cliente = tblClientes.getSelectionModel().getSelectedItem();
        if (cliente == null) return;
        if (cliente.getDirecciones() == null || cliente.getDirecciones().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "El cliente no tiene direcciones registradas.").showAndWait();
            return;
        }
        var direccion = cliente.getDirecciones().get(0);
        if (direccion.getLatitud() == null || direccion.getLongitud() == null) {
            new Alert(Alert.AlertType.WARNING, "Esta dirección no tiene coordenadas registradas.").showAndWait();
            return;
        }
        ni.edu.uam.proyectofinalpae.map.MapaDireccionDialog.ver(
                btnVerMapa.getScene().getWindow(), direccion.getNombre(),
                direccion.getLatitud(), direccion.getLongitud());
    }
}