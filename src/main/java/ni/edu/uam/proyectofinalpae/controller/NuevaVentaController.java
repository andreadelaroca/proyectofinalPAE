package ni.edu.uam.proyectofinalpae.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ni.edu.uam.proyectofinalpae.model.*;
import ni.edu.uam.proyectofinalpae.service.*;
import ni.edu.uam.proyectofinalpae.util.PestanaManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Formulario para registrar ventas directas o facturar pedidos pendientes mediante VentaService. */
public class NuevaVentaController {
    @FXML private ComboBox<Pedido> cbPedido;
    @FXML private ComboBox<Cliente> cbCliente;
    @FXML private ComboBox<Colaborador> cbColaborador;
    @FXML private ComboBox<Producto> cbProducto;
    @FXML private ComboBox<MetodoPago> cbMetodoPago;
    @FXML private Spinner<Integer> spCantidad;
    @FXML private TextArea txtDetalle;
    @FXML private Label lblMensaje;
    @FXML private Button btnAgregar, btnGuardar, btnCancelar;
    private final Map<Producto, Integer> items = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        try {
            cbPedido.setItems(FXCollections.observableArrayList(new PedidoService().obtenerPendientesDeFacturar()));
            cbCliente.setItems(FXCollections.observableArrayList(new ClienteService().obtenerTodos()));
            cbColaborador.setItems(FXCollections.observableArrayList(new ColaboradorService().obtenerTodos()));
            cbProducto.setItems(FXCollections.observableArrayList(new ProductoService().obtenerTodos()));
            cbMetodoPago.setItems(FXCollections.observableArrayList(MetodoPago.values()));
            cbMetodoPago.setValue(MetodoPago.EFECTIVO);
            spCantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100000, 1));

            cbPedido.setConverter(conversor(p -> "Pedido #" + p.getId() + " - " + p.getCliente().getNombres() + " ($" + p.getPrecioTotal() + ")"));
            cbCliente.setConverter(conversor(c -> c.getId() + " - " + c.getNombres() + " " + c.getApellidos()));
            cbColaborador.setConverter(conversor(c -> c.getId() + " - " + c.getNombres() + " " + c.getApellidos()));
            cbProducto.setConverter(conversor(p -> p.getCodigo() + " - " + p.getNombre() + " (stock " + p.getCantidad() + ")"));

            cbPedido.valueProperty().addListener((obs, anterior, pedido) -> {
                boolean esPedido = (pedido != null);
                cbCliente.setDisable(esPedido);
                cbProducto.setDisable(esPedido);
                spCantidad.setDisable(esPedido);
                btnAgregar.setDisable(esPedido);
                items.clear();
                lblMensaje.setText("");

                if (esPedido) {
                    cbCliente.setValue(pedido.getCliente());
                    if (pedido.getColaborador() != null) {
                        cbColaborador.setValue(pedido.getColaborador());
                    }
                    String resumen = pedido.getDetalles().stream()
                            .map(d -> d.getProducto().getCodigo() + " - " + d.getProducto().getNombre()
                                    + " x " + d.getCantidad() + " ($" + d.getPrecioUnitario() + " c/u)")
                            .collect(Collectors.joining("\n"));
                    txtDetalle.setText(resumen + "\n\nSubtotal pedido: $" + pedido.getPrecioTotal() + " (+ 15% IVA al facturar)");
                } else {
                    cbCliente.setValue(null);
                    txtDetalle.clear();
                }
            });

            btnAgregar.setOnAction(e -> agregar());
            btnGuardar.setOnAction(e -> guardar());
            btnCancelar.setOnAction(e -> cerrar());
            txtDetalle.setEditable(false);
        } catch (Exception e) {
            lblMensaje.setText("Error al obtener datos: " + e.getMessage());
            btnGuardar.setDisable(true);
        }
    }

    private static <T> javafx.util.StringConverter<T> conversor(java.util.function.Function<T, String> display) {
        return new javafx.util.StringConverter<>() {
            @Override public String toString(T value) { return value == null ? "" : display.apply(value); }
            @Override public T fromString(String value) { return null; }
        };
    }

    private void agregar() {
        Producto p = cbProducto.getValue();
        if (p == null) { lblMensaje.setText("Seleccione un producto."); return; }
        int n = spCantidad.getValue();
        int acumulado = items.getOrDefault(p, 0) + n;
        if (acumulado > p.getCantidad()) { lblMensaje.setText("Stock insuficiente para " + p.getNombre()); return; }
        items.put(p, acumulado);
        txtDetalle.setText(items.entrySet().stream()
                .map(e -> e.getKey().getCodigo() + " - " + e.getKey().getNombre() + " x " + e.getValue())
                .collect(Collectors.joining("\n")));
        lblMensaje.setText("");
    }

    private void guardar() {
        if (cbColaborador.getValue() == null) { lblMensaje.setText("Seleccione un colaborador."); return; }

        Pedido pedidoSeleccionado = cbPedido.getValue();
        if (pedidoSeleccionado == null && items.isEmpty()) {
            lblMensaje.setText("Agregue al menos un producto o seleccione un pedido.");
            return;
        }

        try {
            VentaService ventaService = new VentaService();
            Venta guardada;

            if (pedidoSeleccionado != null) {
                guardada = ventaService.facturarPedido(
                        pedidoSeleccionado.getId(),
                        cbColaborador.getValue().getId(),
                        cbMetodoPago.getValue()
                );
            } else {
                Venta venta = new Venta();
                venta.setCliente(cbCliente.getValue());
                venta.setColaborador(cbColaborador.getValue());
                venta.setMetodoPago(cbMetodoPago.getValue());
                for (var item : items.entrySet()) {
                    venta.agregarDetalle(new DetalleVenta(item.getKey(), item.getValue()));
                }
                guardada = ventaService.registrarVenta(venta);
            }

            new Alert(Alert.AlertType.INFORMATION, "Venta registrada: " + guardada.getNumeroFactura()).showAndWait();
            cerrar();
        } catch (Exception ex) {
            lblMensaje.setText("No se pudo registrar: " + ex.getMessage());
        }
    }

    private void cerrar() {
        PestanaManager.cerrar(btnCancelar);
    }
}