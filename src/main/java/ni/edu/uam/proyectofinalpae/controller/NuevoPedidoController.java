package ni.edu.uam.proyectofinalpae.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import ni.edu.uam.proyectofinalpae.model.Cliente;
import ni.edu.uam.proyectofinalpae.model.Colaborador;
import ni.edu.uam.proyectofinalpae.model.DetallePedido;
import ni.edu.uam.proyectofinalpae.model.Direccion;
import ni.edu.uam.proyectofinalpae.model.Pedido;
import ni.edu.uam.proyectofinalpae.model.Producto;
import ni.edu.uam.proyectofinalpae.service.DireccionService;
import ni.edu.uam.proyectofinalpae.service.PedidoService;
import ni.edu.uam.proyectofinalpae.service.ProductoService;
import ni.edu.uam.proyectofinalpae.util.Async;
import ni.edu.uam.proyectofinalpae.util.Ui;

import java.math.BigDecimal;
import java.time.LocalDate;

public class NuevoPedidoController {

    @FXML private ComboBox<Cliente> cbCliente;
    @FXML private ComboBox<Direccion> cbDireccionEntrega;   // NUEVO: direcciones del cliente elegido
    @FXML private ComboBox<Colaborador> cbColaborador;      // ahora opcional
    @FXML private DatePicker dpFechaEntrega;
    @FXML private TextField txtDescripcion;

    @FXML private TextField txtCodigoProducto;
    @FXML private Button btnBuscarProducto;
    @FXML private TextField txtCantidad;
    @FXML private Button btnAgregarProducto;
    @FXML private Button btnQuitarProducto;
    @FXML private Label lblInfoProducto;

    @FXML private TableView<DetallePedido> tblDetalles;
    @FXML private TableColumn<DetallePedido, String> colCodigo;
    @FXML private TableColumn<DetallePedido, String> colProducto;
    @FXML private TableColumn<DetallePedido, Integer> colCantidad;
    @FXML private TableColumn<DetallePedido, BigDecimal> colPrecioUnit;
    @FXML private TableColumn<DetallePedido, BigDecimal> colSubtotal;

    @FXML private Label lblMensaje;
    @FXML private Label lblTotal;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardar;

    private final PedidoService pedidoService = new PedidoService();
    private final ProductoService productoService = new ProductoService();
    private final DireccionService direccionService = new DireccionService();

    private final Pedido pedidoActual = new Pedido();
    private final ObservableList<DetallePedido> detallesObservable = FXCollections.observableArrayList();
    private final ObservableList<Cliente> clientes = FXCollections.observableArrayList();
    private Producto productoSeleccionado;

    @FXML
    public void initialize() {
        lblMensaje.setText("");
        dpFechaEntrega.setValue(LocalDate.now().plusDays(3));
        txtCantidad.setText("1");
        Ui.campoCodigoProducto(txtCodigoProducto);

        configurarCombos();
        configurarTabla();

        btnQuitarProducto.disableProperty().bind(tblDetalles.getSelectionModel().selectedItemProperty().isNull());

        btnBuscarProducto.setOnAction(e -> buscarProducto());
        txtCodigoProducto.setOnAction(e -> buscarProducto());
        btnAgregarProducto.setOnAction(e -> agregarProductoAlPedido());
        btnQuitarProducto.setOnAction(e -> quitarDetalleSeleccionado());
        btnCancelar.setOnAction(e -> cerrarVentana());
        btnGuardar.setOnAction(e -> guardarPedido());
    }

    private void configurarCombos() {
        FilteredList<Cliente> clientesFiltrados = new FilteredList<>(clientes, p -> true);

        cbCliente.setEditable(true);
        cbCliente.setVisibleRowCount(6);
        cbCliente.setItems(clientesFiltrados);

        StringConverter<Cliente> clienteConverter = new StringConverter<>() {
            @Override
            public String toString(Cliente c) {
                return c == null ? "" : c.getNombres() + " (" + c.getUsername() + ")";
            }

            @Override
            public Cliente fromString(String texto) {
                if (texto == null || texto.isBlank()) return null;
                return clientes.stream()
                        .filter(c -> toString(c).equalsIgnoreCase(texto.trim()))
                        .findFirst().orElse(null);
            }
        };
        cbCliente.setConverter(clienteConverter);

        cbCliente.getEditor().textProperty().addListener((obs, textoAnterior, textoNuevo) -> {
            final TextField editor = cbCliente.getEditor();
            final Cliente seleccionado = cbCliente.getSelectionModel().getSelectedItem();

            Platform.runLater(() -> {
                if (seleccionado != null && clienteConverter.toString(seleccionado).equals(textoNuevo)) return;
                if (seleccionado != null) cbCliente.getSelectionModel().clearSelection();

                String filtro = (textoNuevo == null) ? "" : textoNuevo.toLowerCase().trim();
                clientesFiltrados.setPredicate(c -> filtro.isEmpty()
                        || clienteConverter.toString(c).toLowerCase().contains(filtro));

                if (!clientesFiltrados.isEmpty() && editor.isFocused()) cbCliente.show();
                else cbCliente.hide();
            });
        });

        // Al elegir cliente se cargan SUS direcciones para la entrega
        cbCliente.getSelectionModel().selectedItemProperty().addListener((obs, anterior, cliente) -> cargarDirecciones(cliente));

        cbDireccionEntrega.setConverter(new StringConverter<>() {
            @Override
            public String toString(Direccion d) {
                return d == null ? "" : d.getNombre();
            }

            @Override
            public Direccion fromString(String s) {
                return null;
            }
        });

        cbColaborador.setPromptText("Sin asignar (opcional)");
        cbColaborador.setConverter(new StringConverter<>() {
            @Override
            public String toString(Colaborador c) {
                return c == null ? "" : c.getNombres() + " (" + c.getUsername() + ")";
            }

            @Override
            public Colaborador fromString(String s) {
                return null;
            }
        });

        // Carga de BD fuera del hilo de JavaFX
        Async.ejecutar(pedidoService::obtenerClientes, clientes::setAll,
                error -> lblMensaje.setText("No se pudieron cargar los clientes: " + Async.mensaje(error)));
        Async.ejecutar(pedidoService::obtenerColaboradores,
                lista -> cbColaborador.setItems(FXCollections.observableArrayList(lista)),
                error -> lblMensaje.setText("No se pudieron cargar los colaboradores: " + Async.mensaje(error)));
    }

    private void cargarDirecciones(Cliente cliente) {
        cbDireccionEntrega.getItems().clear();
        if (cliente == null) return;
        Async.ejecutar(() -> direccionService.obtenerPorCliente(cliente.getId()), lista -> {
            cbDireccionEntrega.setItems(FXCollections.observableArrayList(lista));
            if (!lista.isEmpty()) cbDireccionEntrega.getSelectionModel().selectFirst();
        }, error -> lblMensaje.setText(Async.mensaje(error)));
    }

    private void configurarTabla() {
        colCodigo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProducto().getCodigo()));
        colProducto.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProducto().getNombre()));
        colCantidad.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getCantidad()));
        colPrecioUnit.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getPrecioUnitario()));
        colSubtotal.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getSubtotal()));
        tblDetalles.setItems(detallesObservable);
    }

    private void buscarProducto() {
        lblMensaje.setText("");
        String codigo = txtCodigoProducto.getText().trim();

        if (codigo.isEmpty() || codigo.equalsIgnoreCase("BUN-")) {
            productoSeleccionado = null;
            lblInfoProducto.setText("Complete el número del código (ej. BUN-001).");
            return;
        }

        Async.ejecutar(() -> productoService.buscarPorCodigo(codigo), encontrado -> {
            if (encontrado == null) {
                productoSeleccionado = null;
                lblInfoProducto.setText("No existe ningún producto con el código: " + codigo.toUpperCase());
            } else {
                productoSeleccionado = encontrado;
                lblInfoProducto.setText(String.format("%s — Precio: $%s | Stock disponible: %d",
                        encontrado.getNombre(), encontrado.getPrecioUnitario(), encontrado.getCantidad()));
            }
        }, error -> lblInfoProducto.setText(Async.mensaje(error)));
    }

    private void agregarProductoAlPedido() {
        lblMensaje.setText("");

        if (productoSeleccionado == null
                || !productoSeleccionado.getCodigo().equalsIgnoreCase(txtCodigoProducto.getText().trim())) {
            lblMensaje.setText("Busque primero un código de producto válido (botón Buscar o Enter).");
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(txtCantidad.getText().trim());
            if (cantidad <= 0) {
                lblMensaje.setText("La cantidad debe ser mayor a 0.");
                return;
            }
        } catch (NumberFormatException e) {
            lblMensaje.setText("Ingrese una cantidad entera válida.");
            return;
        }

        DetallePedido existente = detallesObservable.stream()
                .filter(d -> d.getProducto().getId().equals(productoSeleccionado.getId()))
                .findFirst().orElse(null);

        int cantidadTotal = cantidad + (existente != null ? existente.getCantidad() : 0);
        if (cantidadTotal > productoSeleccionado.getCantidad()) {
            lblMensaje.setText("Stock insuficiente. Disponible: " + productoSeleccionado.getCantidad());
            return;
        }

        if (existente != null) {
            existente.setCantidad(cantidadTotal);
            existente.calcularSubtotal();
            pedidoActual.recalcularTotal();      // ya no se re-agrega y quita el detalle
            tblDetalles.refresh();
        } else {
            DetallePedido nuevo = new DetallePedido(productoSeleccionado, cantidad);
            pedidoActual.agregarDetalle(nuevo);
            detallesObservable.add(nuevo);
        }

        actualizarTotalVisual();
        txtCodigoProducto.setText("BUN-");
        txtCodigoProducto.positionCaret(4);
        txtCodigoProducto.requestFocus();
        txtCantidad.setText("1");
        productoSeleccionado = null;
        lblInfoProducto.setText("Producto agregado. Ingrese otro código o guarde el pedido.");
    }

    private void quitarDetalleSeleccionado() {
        DetallePedido seleccionado = tblDetalles.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;
        pedidoActual.removerDetalle(seleccionado);
        detallesObservable.remove(seleccionado);
        actualizarTotalVisual();
    }

    private void actualizarTotalVisual() {
        lblTotal.setText("Total: $" + pedidoActual.getPrecioTotal());
    }

    private void guardarPedido() {
        lblMensaje.setText("");

        Cliente cliente = cbCliente.getSelectionModel().getSelectedItem();
        Direccion direccion = cbDireccionEntrega.getValue();
        Colaborador colaborador = cbColaborador.getValue();   // puede ser null
        LocalDate fechaEntrega = dpFechaEntrega.getValue();
        String descripcion = txtDescripcion.getText().trim();

        if (cliente == null) { lblMensaje.setText("Seleccione un cliente válido de la lista."); return; }
        if (direccion == null) { lblMensaje.setText("Seleccione la dirección de entrega."); return; }
        if (fechaEntrega == null || fechaEntrega.isBefore(LocalDate.now())) {
            lblMensaje.setText("Seleccione una fecha de entrega válida (hoy o posterior).");
            return;
        }
        if (descripcion.isEmpty()) { lblMensaje.setText("Ingrese una descripción para el pedido."); return; }
        if (descripcion.length() > 150) { lblMensaje.setText("La descripción no puede superar los 150 caracteres."); return; }
        if (detallesObservable.isEmpty()) { lblMensaje.setText("Debe agregar al menos un producto al pedido."); return; }

        pedidoActual.setCliente(cliente);
        pedidoActual.setColaborador(colaborador);
        pedidoActual.setDireccionEntrega(direccion);
        pedidoActual.setFechaPedido(LocalDate.now());
        pedidoActual.setFechaEntrega(fechaEntrega);
        pedidoActual.setDescripcion(descripcion);
        // el estado inicial (PENDIENTE) ya viene por defecto en la entidad

        btnGuardar.setDisable(true);
        Async.ejecutar(() -> pedidoService.registrarPedido(pedidoActual), guardado -> {
            Ui.info("Pedido Registrado", "El pedido fue registrado y el inventario fue actualizado correctamente.");
            cerrarVentana();
        }, error -> {
            btnGuardar.setDisable(false);
            lblMensaje.setText("Error al registrar el pedido: " + Async.mensaje(error));
        });
    }

    private void cerrarVentana() {
        ((Stage) btnCancelar.getScene().getWindow()).close();
    }
}