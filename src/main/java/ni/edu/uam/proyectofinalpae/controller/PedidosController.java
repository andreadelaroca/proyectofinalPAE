package ni.edu.uam.proyectofinalpae.controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ni.edu.uam.proyectofinalpae.model.Colaborador;
import ni.edu.uam.proyectofinalpae.model.Direccion;
import ni.edu.uam.proyectofinalpae.model.EstadoPedido;
import ni.edu.uam.proyectofinalpae.model.Pedido;
import ni.edu.uam.proyectofinalpae.service.PedidoService;
import ni.edu.uam.proyectofinalpae.util.Async;
import ni.edu.uam.proyectofinalpae.util.Ui;
import ni.edu.uam.proyectofinalpae.util.Vistas;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Antes: GestionPedidosController (renombrado para coincidir con pedidos-view.fxml). */
public class PedidosController {

    @FXML private Button btnNuevoPedido;
    @FXML private Button btnActualizarEstado;
    @FXML private Button btnAsignarColaborador;  // NUEVO: el colaborador ahora es opcional al crear el pedido
    @FXML private Button btnVerEntrega;          // NUEVO: dirección de entrega en OpenStreetMap
    @FXML private Button btnCobrar;              // NUEVO: convierte el pedido en una venta
    @FXML private Button btnRefrescar;
    @FXML private TextField txtBuscar;

    @FXML private TableView<Pedido> tblPedidos;
    @FXML private TableColumn<Pedido, Long> colIdPedido;
    @FXML private TableColumn<Pedido, LocalDate> colFechaCompra;
    @FXML private TableColumn<Pedido, String> colEstado;
    @FXML private TableColumn<Pedido, BigDecimal> colTotal;
    @FXML private TableColumn<Pedido, String> colDescripcion;
    @FXML private TableColumn<Pedido, String> colUsuarioCliente;
    @FXML private TableColumn<Pedido, String> colUsuarioColaborador;

    private final PedidoService pedidoService = new PedidoService();
    private final ObservableList<Pedido> listaPedidos = FXCollections.observableArrayList();
    private FilteredList<Pedido> listaFiltrada;

    @FXML
    public void initialize() {
        configurarColumnas();

        listaFiltrada = new FilteredList<>(listaPedidos, p -> true);
        tblPedidos.setItems(listaFiltrada);
        configurarFiltro();

        tblPedidos.getSelectionModel().selectedItemProperty().addListener((obs, a, p) -> actualizarBotones(p));
        actualizarBotones(null);

        btnNuevoPedido.setOnAction(e -> abrirNuevoPedido());
        btnActualizarEstado.setOnAction(e -> actualizarEstadoPedido());
        btnAsignarColaborador.setOnAction(e -> asignarColaborador());
        btnVerEntrega.setOnAction(e -> verEntrega());
        btnCobrar.setOnAction(e -> cobrarPedido());
        btnRefrescar.setOnAction(e -> cargarPedidos());

        cargarPedidos();
    }

    private void configurarColumnas() {
        colIdPedido.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getId()));
        colFechaCompra.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getFechaPedido()));
        colEstado.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEstado().toString()));
        colTotal.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getPrecioTotal()));
        colDescripcion.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescripcion()));
        colUsuarioCliente.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getCliente().getUsername()));
        // colaborador es opcional en el modelo
        colUsuarioColaborador.setCellValueFactory(cell -> {
            Colaborador c = cell.getValue().getColaborador();
            return new SimpleStringProperty(c == null ? "Sin asignar" : c.getUsername());
        });
    }

    private void actualizarBotones(Pedido p) {
        boolean sinSeleccion = p == null;
        boolean cerrado = sinSeleccion || p.getEstado() == EstadoPedido.CANCELADO
                || p.getEstado() == EstadoPedido.ENTREGADO;
        btnActualizarEstado.setDisable(sinSeleccion);
        btnVerEntrega.setDisable(sinSeleccion);
        btnAsignarColaborador.setDisable(cerrado);
        btnCobrar.setDisable(cerrado);
    }

    private void cargarPedidos() {
        tblPedidos.setPlaceholder(new Label("Cargando pedidos..."));
        btnRefrescar.setDisable(true);

        Async.ejecutar(pedidoService::obtenerTodos, lista -> {
            listaPedidos.setAll(lista);
            tblPedidos.setPlaceholder(new Label("No hay pedidos registrados."));
            btnRefrescar.setDisable(false);
        }, error -> {
            tblPedidos.setPlaceholder(new Label("Error al cargar los pedidos."));
            btnRefrescar.setDisable(false);
            Async.mensaje(error);
        });
    }

    private void actualizarEstadoPedido() {
        Pedido seleccionado = tblPedidos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        ChoiceDialog<EstadoPedido> dialogo = new ChoiceDialog<>(seleccionado.getEstado(), EstadoPedido.values());
        dialogo.setTitle("Actualizar Estado");
        dialogo.setHeaderText("Pedido #" + seleccionado.getId() + " - " + seleccionado.getCliente().getNombres());
        dialogo.setContentText("Seleccione el nuevo estado:");

        dialogo.showAndWait().ifPresent(nuevo -> {
            if (nuevo == seleccionado.getEstado()) return;
            if (nuevo == EstadoPedido.CANCELADO && !Ui.confirmar("Cancelar pedido",
                    "Se cancelará el pedido y las existencias volverán al inventario. ¿Continuar?")) {
                return;
            }
            Async.ejecutarSinResultado(
                    () -> pedidoService.actualizarEstado(seleccionado.getId(), nuevo),
                    this::cargarPedidos, // recarga: cancelar cambia también el stock
                    error -> Ui.error("Error", "No se pudo actualizar el estado: " + Async.mensaje(error)));
        });
    }

    private void asignarColaborador() {
        Pedido seleccionado = tblPedidos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        Async.ejecutar(pedidoService::obtenerColaboradores, colaboradores -> {
            if (colaboradores.isEmpty()) {
                Ui.advertencia("Sin colaboradores", "No hay colaboradores registrados.");
                return;
            }
            // ChoiceDialog usa toString(); se muestran etiquetas y se busca el colaborador por etiqueta
            Map<String, Colaborador> opciones = new LinkedHashMap<>();
            colaboradores.forEach(c -> opciones.put(c.getNombres() + " " + c.getApellidos() + " (" + c.getUsername() + ")", c));

            ChoiceDialog<String> dialogo = new ChoiceDialog<>(opciones.keySet().iterator().next(), opciones.keySet());
            dialogo.setTitle("Asignar colaborador");
            dialogo.setHeaderText("Pedido #" + seleccionado.getId());
            dialogo.setContentText("Colaborador:");

            dialogo.showAndWait().ifPresent(etiqueta -> Async.ejecutarSinResultado(
                    () -> pedidoService.asignarColaborador(seleccionado.getId(), opciones.get(etiqueta).getId()),
                    this::cargarPedidos,
                    error -> Ui.error("Error", Async.mensaje(error))));
        }, error -> Ui.error("Error", Async.mensaje(error)));
    }

    private void verEntrega() {
        Pedido seleccionado = tblPedidos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        Direccion d = seleccionado.getDireccionEntrega();
        if (d == null) {
            Ui.advertencia("Sin dirección", "Este pedido no tiene dirección de entrega.");
            return;
        }
        MapaDireccionController.ver(btnVerEntrega.getScene().getWindow(), d.getNombre(), d.getLatitud(), d.getLongitud());
    }

    private void cobrarPedido() {
        Pedido seleccionado = tblPedidos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;
        try {
            Vistas.Vista<NuevaVentaController> vista = Vistas.cargar(Vistas.NUEVA_VENTA);
            vista.controller().cargarDesdePedido(seleccionado.getId());
            Vistas.mostrarModal(vista, "BunBun Hub - Cobrar pedido #" + seleccionado.getId(),
                    btnCobrar.getScene().getWindow());
            cargarPedidos();
        } catch (IOException e) {
            e.printStackTrace();
            Ui.error("Error", "No se pudo abrir la ventana de venta.");
        }
    }

    private void abrirNuevoPedido() {
        try {
            Vistas.Vista<NuevoPedidoController> vista = Vistas.cargar(Vistas.NUEVO_PEDIDO);
            Vistas.mostrarModal(vista, "BunBun Hub - Nuevo Pedido", btnNuevoPedido.getScene().getWindow());
            cargarPedidos();
        } catch (IOException e) {
            e.printStackTrace();
            Ui.error("Error", "No se pudo abrir la ventana de nuevo pedido.");
        }
    }

    private void configurarFiltro() {
        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> {
            String filtro = (nuevo == null) ? "" : nuevo.toLowerCase().trim();
            listaFiltrada.setPredicate(pedido -> {
                if (filtro.isEmpty()) return true;

                String id = pedido.getId() != null ? String.valueOf(pedido.getId()) : "";
                String cliente = (pedido.getCliente().getNombres() + " " + pedido.getCliente().getApellidos()
                        + " " + pedido.getCliente().getUsername()).toLowerCase();
                Colaborador c = pedido.getColaborador();
                String colaborador = c == null ? "sin asignar"
                        : (c.getNombres() + " " + c.getApellidos() + " " + c.getUsername()).toLowerCase();
                String estado = pedido.getEstado().toString().toLowerCase();

                return id.contains(filtro) || cliente.contains(filtro)
                        || colaborador.contains(filtro) || estado.contains(filtro);
            });
        });
    }
}