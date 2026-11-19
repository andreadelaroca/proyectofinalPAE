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
import ni.edu.uam.proyectofinalpae.model.Cliente;
import ni.edu.uam.proyectofinalpae.model.Direccion;
import ni.edu.uam.proyectofinalpae.service.ClienteService;
import ni.edu.uam.proyectofinalpae.util.Async;
import ni.edu.uam.proyectofinalpae.util.Ui;
import ni.edu.uam.proyectofinalpae.util.Vistas;

import java.io.IOException;

public class ClientesController {

    @FXML private TextField txtBuscar;
    @FXML private Button btnRefrescar;
    @FXML private Button btnNuevoCliente;
    @FXML private Button btnVerMapa;   // NUEVO: ver la dirección del cliente seleccionado en OpenStreetMap

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

        btnVerMapa.disableProperty().bind(tblClientes.getSelectionModel().selectedItemProperty().isNull());
        btnRefrescar.setOnAction(e -> cargarClientes());
        btnNuevoCliente.setOnAction(e -> abrirNuevoCliente());
        btnVerMapa.setOnAction(e -> verEnMapa());

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
                return contiene(cli.getNombres(), filtro) || contiene(cli.getApellidos(), filtro)
                        || contiene(cli.getUsername(), filtro) || contiene(cli.getCorreo(), filtro);
            });
        });
    }

    private boolean contiene(String texto, String filtro) {
        return texto != null && texto.toLowerCase().contains(filtro);
    }

    private void cargarClientes() {
        tblClientes.setPlaceholder(new Label("Cargando clientes..."));
        Async.ejecutar(clienteService::obtenerTodos, lista -> {
            listaClientes.setAll(lista);
            tblClientes.setPlaceholder(new Label("No hay clientes registrados."));
        }, error -> {
            tblClientes.setPlaceholder(new Label("Error al cargar los clientes."));
            Async.mensaje(error);
        });
    }

    private void abrirNuevoCliente() {
        try {
            Vistas.Vista<NuevoClienteController> vista = Vistas.cargar(Vistas.NUEVO_CLIENTE);
            Vistas.mostrarModal(vista, "BunBun Hub - Nuevo Cliente", btnNuevoCliente.getScene().getWindow());

            Cliente creado = vista.controller().getClienteCreado();
            if (creado != null) listaClientes.add(creado);
        } catch (IOException e) {
            e.printStackTrace();
            Ui.error("Error", "No se pudo abrir el formulario de cliente.");
        }
    }

    private void verEnMapa() {
        Cliente cliente = tblClientes.getSelectionModel().getSelectedItem();
        if (cliente == null) return;
        if (cliente.getDirecciones() == null || cliente.getDirecciones().isEmpty()) {
            Ui.advertencia("Sin dirección", "Este cliente no tiene direcciones registradas.");
            return;
        }
        Direccion d = cliente.getDirecciones().get(0);
        MapaDireccionController.ver(btnVerMapa.getScene().getWindow(), d.getNombre(), d.getLatitud(), d.getLongitud());
    }
}