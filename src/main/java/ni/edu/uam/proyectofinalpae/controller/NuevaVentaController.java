package ni.edu.uam.proyectofinalpae.controller;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import ni.edu.uam.proyectofinalpae.model.Cliente;
import ni.edu.uam.proyectofinalpae.model.Colaborador;
import ni.edu.uam.proyectofinalpae.model.DetallePedido;
import ni.edu.uam.proyectofinalpae.model.DetalleVenta;
import ni.edu.uam.proyectofinalpae.model.MetodoPago;
import ni.edu.uam.proyectofinalpae.model.Producto;
import ni.edu.uam.proyectofinalpae.model.Venta;
import ni.edu.uam.proyectofinalpae.service.PedidoService;
import ni.edu.uam.proyectofinalpae.service.ProductoService;
import ni.edu.uam.proyectofinalpae.service.VentaService;
import ni.edu.uam.proyectofinalpae.util.Async;
import ni.edu.uam.proyectofinalpae.util.Sesion;
import ni.edu.uam.proyectofinalpae.util.Ui;

import java.math.BigDecimal;

/** Formulario de venta (nueva-venta-view.fxml). Puede abrirse vacío o desde un pedido (cargarDesdePedido). */
public class NuevaVentaController {

    @FXML private ComboBox<Cliente> cbCliente;          // opcional: vacío = consumidor final
    @FXML private ComboBox<MetodoPago> cbMetodoPago;
    @FXML private Pane pnlAgregarProducto;              // contenedor de código/cantidad/botones (se deshabilita en modo pedido)
    @FXML private TextField txtCodigoProducto;
    @FXML private Button btnBuscarProducto;
    @FXML private TextField txtCantidad;
    @FXML private Button btnAgregarProducto;
    @FXML private Button btnQuitarProducto;
    @FXML private Label lblInfoProducto;

    @FXML private TableView<DetalleVenta> tblDetalles;
    @FXML private TableColumn<DetalleVenta, String> colCodigo;
    @FXML private TableColumn<DetalleVenta, String> colProducto;
    @FXML private TableColumn<DetalleVenta, Integer> colCantidad;
    @FXML private TableColumn<DetalleVenta, BigDecimal> colPrecioUnit;
    @FXML private TableColumn<DetalleVenta, BigDecimal> colSubtotal;

    @FXML private Label lblSubtotal, lblImpuesto, lblTotal, lblMensaje;
    @FXML private Button btnCancelar, btnGuardar;

    private final VentaService ventaService = new VentaService();
    private final ProductoService productoService = new ProductoService();
    private final PedidoService pedidoService = new PedidoService();

    private final Venta ventaActual = new Venta();
    private final ObservableList<DetalleVenta> detallesObservable = FXCollections.observableArrayList();
    private final SimpleBooleanProperty modoPedido = new SimpleBooleanProperty(false);
    private Producto productoSeleccionado;

    @FXML
    public void initialize() {
        lblMensaje.setText("");
        txtCantidad.setText("1");
        Ui.campoCodigoProducto(txtCodigoProducto);

        cbMetodoPago.setItems(FXCollections.observableArrayList(MetodoPago.values()));
        cbMetodoPago.setValue(MetodoPago.EFECTIVO);
        cbCliente.setPromptText("Consumidor final");
        cbCliente.setConverter(new StringConverter<>() {
            @Override
            public String toString(Cliente c) {
                return c == null ? "" : c.getNombres() + " " + c.getApellidos() + " (" + c.getUsername() + ")";
            }

            @Override
            public Cliente fromString(String s) {
                return null;
            }
        });
        Async.ejecutar(pedidoService::obtenerClientes,
                lista -> cbCliente.setItems(FXCollections.observableArrayList(lista)),
                error -> lblMensaje.setText("No se pudieron cargar los clientes: " + Async.mensaje(error)));

        configurarTabla();
        btnQuitarProducto.disableProperty().bind(
                tblDetalles.getSelectionModel().selectedItemProperty().isNull().or(modoPedido));

        btnBuscarProducto.setOnAction(e -> buscarProducto());
        txtCodigoProducto.setOnAction(e -> buscarProducto());
        btnAgregarProducto.setOnAction(e -> agregarProducto());
        btnQuitarProducto.setOnAction(e -> quitarProducto());
        btnCancelar.setOnAction(e -> cerrarVentana());
        btnGuardar.setOnAction(e -> guardarVenta());
        actualizarTotales();
    }

    /**
     * Cobra un pedido existente: copia sus líneas con los precios del pedido, bloquea la edición
     * y NO vuelve a descontar stock (ya se descontó al crear el pedido).
     */
    public void cargarDesdePedido(Long idPedido) {
        modoPedido.set(true);
        pnlAgregarProducto.setDisable(true);
        cbCliente.setDisable(true);

        Async.ejecutar(() -> pedidoService.obtenerConDetalles(idPedido), pedido -> {
            ventaActual.setPedido(pedido);
            cbCliente.setValue(pedido.getCliente());
            for (DetallePedido dp : pedido.getDetalles()) {
                DetalleVenta dv = new DetalleVenta(dp.getProducto(), dp.getCantidad());
                dv.setPrecioUnitario(dp.getPrecioUnitario());   // respeta el precio del pedido
                ventaActual.agregarDetalle(dv);
                detallesObservable.add(dv);
            }
            actualizarTotales();
        }, error -> {
            lblMensaje.setText(Async.mensaje(error));
            btnGuardar.setDisable(true);
        });
    }

    private void configurarTabla() {
        colCodigo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProducto().getCodigo()));
        colProducto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProducto().getNombre()));
        colCantidad.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCantidad()));
        colPrecioUnit.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrecioUnitario()));
        colSubtotal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getSubtotal()));
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
            productoSeleccionado = encontrado;
            lblInfoProducto.setText(encontrado == null
                    ? "No existe ningún producto con el código: " + codigo.toUpperCase()
                    : String.format("%s — Precio: $%s | Stock disponible: %d",
                    encontrado.getNombre(), encontrado.getPrecioUnitario(), encontrado.getCantidad()));
        }, error -> lblInfoProducto.setText(Async.mensaje(error)));
    }

    private void agregarProducto() {
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

        DetalleVenta existente = detallesObservable.stream()
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
            ventaActual.recalcularTotales();
            tblDetalles.refresh();
        } else {
            DetalleVenta nuevo = new DetalleVenta(productoSeleccionado, cantidad);
            ventaActual.agregarDetalle(nuevo);
            detallesObservable.add(nuevo);
        }

        actualizarTotales();
        txtCodigoProducto.setText("BUN-");
        txtCodigoProducto.positionCaret(4);
        txtCodigoProducto.requestFocus();
        txtCantidad.setText("1");
        productoSeleccionado = null;
        lblInfoProducto.setText("Producto agregado.");
    }

    private void quitarProducto() {
        DetalleVenta seleccionado = tblDetalles.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;
        ventaActual.removerDetalle(seleccionado);
        detallesObservable.remove(seleccionado);
        actualizarTotales();
    }

    private void actualizarTotales() {
        lblSubtotal.setText("Subtotal: $" + ventaActual.getSubtotal());
        lblImpuesto.setText("IVA (15%): $" + ventaActual.getImpuesto());
        lblTotal.setText("Total: $" + ventaActual.getTotal());
    }

    private void guardarVenta() {
        lblMensaje.setText("");

        Colaborador vendedor = Sesion.getColaborador();
        if (vendedor == null) {
            lblMensaje.setText("Solo un colaborador con sesión iniciada puede registrar ventas.");
            return;
        }
        if (detallesObservable.isEmpty()) {
            lblMensaje.setText("Debe agregar al menos un producto.");
            return;
        }
        if (cbMetodoPago.getValue() == null) {
            lblMensaje.setText("Seleccione el método de pago.");
            return;
        }

        ventaActual.setColaborador(vendedor);
        ventaActual.setMetodoPago(cbMetodoPago.getValue());
        if (!modoPedido.get()) ventaActual.setCliente(cbCliente.getValue());   // null = consumidor final

        btnGuardar.setDisable(true);
        Async.ejecutar(() -> ventaService.registrarVenta(ventaActual), guardada -> {
            Ui.info("Venta registrada", "Factura " + guardada.getNumeroFactura() + " por $" + guardada.getTotal());
            if (Ui.confirmar("Factura", "¿Desea generar la factura en PDF?")) {
                VentaController.exportarFactura(btnGuardar.getScene().getWindow(), guardada.getId());
            }
            cerrarVentana();
        }, error -> {
            btnGuardar.setDisable(false);
            lblMensaje.setText("Error al registrar la venta: " + Async.mensaje(error));
        });
    }

    private void cerrarVentana() {
        ((Stage) btnCancelar.getScene().getWindow()).close();
    }
}