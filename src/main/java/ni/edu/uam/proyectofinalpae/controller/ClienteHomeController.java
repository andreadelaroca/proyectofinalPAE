package ni.edu.uam.proyectofinalpae.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ni.edu.uam.proyectofinalpae.model.Cliente;
import ni.edu.uam.proyectofinalpae.model.Pedido;
import ni.edu.uam.proyectofinalpae.report.ReporteService;
import ni.edu.uam.proyectofinalpae.report.VistaPreviaReporte;
import ni.edu.uam.proyectofinalpae.service.PedidoService;
import ni.edu.uam.proyectofinalpae.util.PestanaManager;
import ni.edu.uam.proyectofinalpae.util.SceneManager;

import java.io.IOException;
import java.util.List;

/** Pantalla del cliente: solo consulta sus pedidos y edita su propio usuario. */
public class ClienteHomeController {

    @FXML private Label lblBienvenida;
    @FXML private Label lblNombre;
    @FXML private Label lblUsername;
    @FXML private Label lblCorreo;
    @FXML private Label lblTelefono;
    @FXML private TextField txtBuscar;
    @FXML private Button btnRefrescar;
    @FXML private Button btnReporte;
    @FXML private Button btnEditarPerfil;
    @FXML private Button btnSalir;

    @FXML private TableView<Pedido> tblPedidos;
    @FXML private TableColumn<Pedido, String> colNumero;
    @FXML private TableColumn<Pedido, String> colFecha;
    @FXML private TableColumn<Pedido, String> colEstado;
    @FXML private TableColumn<Pedido, String> colTotal;

    private final PedidoService pedidoService = new PedidoService();
    private final ReporteService reporteService = new ReporteService();
    private final ObservableList<Pedido> listaPedidos = FXCollections.observableArrayList();
    private FilteredList<Pedido> listaFiltrada;

    private Cliente cliente;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarFiltro();
        tblPedidos.getColumns().forEach(col -> col.setReorderable(false));
    }

    /** Debe llamarse justo después de cargar el FXML, con el cliente que inició sesión. */
    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
        mostrarPerfil();
        cargarPedidos();
    }

    // ---------- Tabla (solo lectura) ----------

    private void configurarTabla() {
        colNumero.setCellValueFactory(c -> new SimpleStringProperty(texto(c.getValue().getId())));
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(texto(c.getValue().getFechaPedido())));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(texto(c.getValue().getEstado())));
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(texto(c.getValue().getPrecioTotal())));

        listaFiltrada = new FilteredList<>(listaPedidos, p -> true);
        tblPedidos.setItems(listaFiltrada);
        tblPedidos.setPlaceholder(new Label("No tienes pedidos registrados."));
    }

    private void configurarFiltro() {
        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> {
            String filtro = (nuevo == null) ? "" : nuevo.toLowerCase().trim();
            listaFiltrada.setPredicate(p -> filtro.isEmpty()
                    || texto(p.getId()).toLowerCase().contains(filtro)
                    || texto(p.getEstado()).toLowerCase().contains(filtro));
        });
    }

    private void cargarPedidos() {
        if (cliente == null) return;
        tblPedidos.setPlaceholder(new Label("Cargando pedidos..."));
        btnRefrescar.setDisable(true);
        final Long idCliente = cliente.getId();

        Task<List<Pedido>> tarea = new Task<>() {
            @Override
            protected List<Pedido> call() {
                // Solo los pedidos de ESTE cliente
                return pedidoService.obtenerPorCliente(idCliente);
            }
        };
        tarea.setOnSucceeded(e -> {
            listaPedidos.setAll(tarea.getValue());
            tblPedidos.setPlaceholder(new Label("No tienes pedidos registrados."));
            btnRefrescar.setDisable(false);
        });
        tarea.setOnFailed(e -> {
            tblPedidos.setPlaceholder(new Label("Error al cargar tus pedidos."));
            btnRefrescar.setDisable(false);
            tarea.getException().printStackTrace();
        });

        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    // ---------- Perfil ----------

    private void mostrarPerfil() {
        if (cliente == null) return;
        lblBienvenida.setText("Hola, " + texto(cliente.getNombres()));
        lblNombre.setText("Nombre: " + texto(cliente.getNombres()) + " " + texto(cliente.getApellidos()));
        lblUsername.setText("Usuario: " + texto(cliente.getUsername()));
        lblCorreo.setText("Correo: " + texto(cliente.getCorreo()));
        lblTelefono.setText("Teléfono: " + texto(cliente.getTelefono()));
    }

    @FXML
    public void editarPerfil() {
        if (cliente == null) return;
        btnEditarPerfil.setDisable(true);
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ni/edu/uam/proyectofinalpae/fxml/nuevo-cliente-view.fxml"));
            Parent root = loader.load();
            NuevoClienteController controller = loader.getController();
            controller.cargarClienteParaEditar(cliente);

            PestanaManager.abrir(root, "Editar mi perfil", btnEditarPerfil, () -> {
                if (controller.isGuardadoExitoso()) {
                    mostrarPerfil();
                }
                btnEditarPerfil.setDisable(false);
            });
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir el formulario de perfil.").showAndWait();
            btnEditarPerfil.setDisable(false);
        }
    }

    @FXML
    public void refrescar() {
        cargarPedidos();
    }

    /** Reporte JasperReports con SOLO los pedidos de este cliente: vista previa y descarga. */
    @FXML
    public void verReporte() {
        if (cliente == null) return;
        final Cliente actual = cliente;
        VistaPreviaReporte.generarYMostrar(btnReporte, "Mis pedidos",
                "mis-pedidos-" + texto(actual.getUsername()) + ".pdf",
                () -> reporteService.generarPedidosCliente(actual));
    }

    @FXML
    public void salir() {
        SceneManager.abrirVentana("/ni/edu/uam/proyectofinalpae/fxml/login.fxml", "BunBun Hub - Ingreso");
    }

    private static String texto(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}