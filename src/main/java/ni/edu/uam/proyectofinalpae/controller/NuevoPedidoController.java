package ni.edu.uam.proyectofinalpae.controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import ni.edu.uam.proyectofinalpae.model.*;
import ni.edu.uam.proyectofinalpae.service.PedidoService;
import ni.edu.uam.proyectofinalpae.service.ProductoService;

import java.math.BigDecimal;
import java.time.LocalDate;

public class NuevoPedidoController {

    @FXML private ComboBox<Cliente> cbCliente;
    @FXML private ComboBox<Colaborador> cbColaborador;
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

    private final Pedido pedidoActual = new Pedido();
    private final ObservableList<DetallePedido> detallesObservable = FXCollections.observableArrayList();
    private Producto productoSeleccionado;

    @FXML
    public void initialize() {
        lblMensaje.setText("");
        dpFechaEntrega.setValue(LocalDate.now().plusDays(3));
        txtCantidad.setText("1");

        configurarCombos();
        configurarTabla();

        btnQuitarProducto.disableProperty().bind(
                tblDetalles.getSelectionModel().selectedItemProperty().isNull()
        );

        btnBuscarProducto.setOnAction(e -> buscarProducto());
        txtCodigoProducto.setOnAction(e -> buscarProducto());
        btnAgregarProducto.setOnAction(e -> agregarProductoAlPedido());
        btnQuitarProducto.setOnAction(e -> quitarDetalleSeleccionado());
        btnCancelar.setOnAction(e -> cerrarVentana());
        btnGuardar.setOnAction(e -> guardarPedido());
    }

    private void configurarCombos() {
        cbCliente.setItems(FXCollections.observableArrayList(pedidoService.obtenerClientes()));
        cbCliente.setConverter(new StringConverter<>() {
            @Override
            public String toString(Cliente c) {
                return c == null ? "" : c.getNombres() + " (" + c.getUsername() + ")";
            }
            @Override
            public Cliente fromString(String s) { return null; }
        });

        cbColaborador.setItems(FXCollections.observableArrayList(pedidoService.obtenerColaboradores()));
        cbColaborador.setConverter(new StringConverter<>() {
            @Override
            public String toString(Colaborador c) {
                return c == null ? "" : c.getNombres() + " (" + c.getUsername() + ")";
            }
            @Override
            public Colaborador fromString(String s) { return null; }
        });
    }

    private void configurarTabla() {
        colCodigo.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getProducto().getCodigo()));
        colProducto.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getProducto().getNombre()));
        colCantidad.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getCantidad()));
        colPrecioUnit.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getPrecioUnitario()));
        colSubtotal.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getSubtotal()));

        tblDetalles.setItems(detallesObservable);
    }

    private void buscarProducto() {
        lblMensaje.setText("");
        String codigo = txtCodigoProducto.getText().trim();

        if (codigo.isEmpty()) {
            productoSeleccionado = null;
            lblInfoProducto.setText("Ingrese un código de producto (ej. BUN-001).");
            return;
        }

        Producto encontrado = productoService.buscarPorCodigo(codigo);
        if (encontrado == null) {
            productoSeleccionado = null;
            lblInfoProducto.setText("No existe ningún producto con el código: " + codigo.toUpperCase());
        } else {
            productoSeleccionado = encontrado;
            lblInfoProducto.setText(String.format("%s — Precio: $%s | Stock disponible: %d",
                    encontrado.getNombre(), encontrado.getPrecioUnitario(), encontrado.getCantidad()));
        }
    }

    private void agregarProductoAlPedido() {
        lblMensaje.setText("");

        // Si escribió un código nuevo y dio directo a Agregar sin pulsar Buscar, lo busca primero
        String codigoEscrito = txtCodigoProducto.getText().trim();
        if (productoSeleccionado == null || !productoSeleccionado.getCodigo().equalsIgnoreCase(codigoEscrito)) {
            buscarProducto();
        }

        if (productoSeleccionado == null) {
            lblMensaje.setText("Debe buscar y seleccionar un código de producto válido.");
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

        // Verifica si el producto ya está en la lista para sumar cantidades
        DetallePedido existente = detallesObservable.stream()
                .filter(d -> d.getProducto().getId().equals(productoSeleccionado.getId()))
                .findFirst()
                .orElse(null);

        int cantidadTotalSolicitada = cantidad + (existente != null ? existente.getCantidad() : 0);
        if (cantidadTotalSolicitada > productoSeleccionado.getCantidad()) {
            lblMensaje.setText("Stock insuficiente. Disponible: " + productoSeleccionado.getCantidad());
            return;
        }

        if (existente != null) {
            existente.setCantidad(cantidadTotalSolicitada);
            existente.calcularSubtotal();
            pedidoActual.agregarDetalle(existente); // Recalcula total
            // Evita duplicar la referencia en pedidoActual.getDetalles()
            pedidoActual.getDetalles().remove(pedidoActual.getDetalles().size() - 1);
            tblDetalles.refresh();
        } else {
            DetallePedido nuevoDetalle = new DetallePedido();
            nuevoDetalle.setProducto(productoSeleccionado);
            nuevoDetalle.setCantidad(cantidad);
            nuevoDetalle.setPrecioUnitario(productoSeleccionado.getPrecioUnitario());

            pedidoActual.agregarDetalle(nuevoDetalle);
            detallesObservable.add(nuevoDetalle);
        }

        actualizarTotalVisual();
        txtCodigoProducto.clear();
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
        BigDecimal total = pedidoActual.getPrecioTotal() != null ? pedidoActual.getPrecioTotal() : BigDecimal.ZERO;
        lblTotal.setText("Total: $" + total);
    }

    private void guardarPedido() {
        lblMensaje.setText("");

        Cliente cliente = cbCliente.getValue();
        Colaborador colaborador = cbColaborador.getValue();
        LocalDate fechaEntrega = dpFechaEntrega.getValue();
        String descripcion = txtDescripcion.getText().trim();

        if (cliente == null) {
            lblMensaje.setText("Seleccione un cliente.");
            return;
        }
        if (colaborador == null) {
            lblMensaje.setText("Seleccione un colaborador.");
            return;
        }
        if (fechaEntrega == null || fechaEntrega.isBefore(LocalDate.now())) {
            lblMensaje.setText("Seleccione una fecha de entrega válida (hoy o posterior).");
            return;
        }
        if (descripcion.isEmpty()) {
            lblMensaje.setText("Ingrese una descripción para el pedido.");
            return;
        }
        if (descripcion.length() > 150) {
            lblMensaje.setText("La descripción no puede superar los 150 caracteres.");
            return;
        }
        if (detallesObservable.isEmpty()) {
            lblMensaje.setText("Debe agregar al menos un producto válido al pedido.");
            return;
        }

        try {
            pedidoActual.setCliente(cliente);
            pedidoActual.setColaborador(colaborador);
            pedidoActual.setFechaPedido(LocalDate.now());
            pedidoActual.setFechaEntrega(fechaEntrega);
            pedidoActual.setDescripcion(descripcion);
            pedidoActual.setEstado("Pendiente");

            pedidoService.registrarPedido(pedidoActual);

            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
            alerta.setTitle("Pedido Registrado");
            alerta.setHeaderText(null);
            alerta.setContentText("El pedido fue registrado y el inventario fue actualizado correctamente.");
            alerta.showAndWait();

            cerrarVentana();
        } catch (Exception e) {
            lblMensaje.setText("Error al registrar el pedido: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }
}