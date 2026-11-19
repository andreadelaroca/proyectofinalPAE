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
import javafx.stage.Window;
import ni.edu.uam.proyectofinalpae.model.EstadoVenta;
import ni.edu.uam.proyectofinalpae.model.Venta;
import ni.edu.uam.proyectofinalpae.report.ReporteService;
import ni.edu.uam.proyectofinalpae.service.VentaService;
import ni.edu.uam.proyectofinalpae.util.Async;
import ni.edu.uam.proyectofinalpae.util.Ui;
import ni.edu.uam.proyectofinalpae.util.Vistas;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

/** Historial de ventas (ventas-view.fxml). */
public class VentasController {
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private TextField txtBuscar;
    @FXML private Button btnRefrescar, btnNuevaVenta, btnAnular, btnFactura;

    @FXML private TableView<Venta> tblVentas;
    @FXML private TableColumn<Venta, String> colNumeroFactura;
    @FXML private TableColumn<Venta, String> colFecha;
    @FXML private TableColumn<Venta, String> colCliente;
    @FXML private TableColumn<Venta, String> colColaborador;
    @FXML private TableColumn<Venta, String> colMetodoPago;
    @FXML private TableColumn<Venta, BigDecimal> colTotal;
    @FXML private TableColumn<Venta, String> colEstado;

    private final VentaService ventaService = new VentaService();
    private final ObservableList<Venta> listaVentas = FXCollections.observableArrayList();
    private FilteredList<Venta> listaFiltrada;

    @FXML
    public void initialize() {
        configurarColumnas();

        listaFiltrada = new FilteredList<>(listaVentas, v -> true);
        tblVentas.setItems(listaFiltrada);
        configurarFiltro();

        tblVentas.getSelectionModel().selectedItemProperty().addListener((obs, a, v) -> actualizarBotones(v));
        actualizarBotones(null);

        btnRefrescar.setOnAction(e -> cargarVentas());
        btnNuevaVenta.setOnAction(e -> abrirNuevaVenta());
        btnAnular.setOnAction(e -> anularVenta());
        btnFactura.setOnAction(e -> exportarFactura(btnFactura.getScene().getWindow(),
                tblVentas.getSelectionModel().getSelectedItem().getId()));

        cargarVentas();
    }

    private void configurarColumnas() {
        colNumeroFactura.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumeroFactura()));
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFecha().format(FECHA_HORA)));
        colCliente.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCliente() == null
                ? "Consumidor final"
                : c.getValue().getCliente().getNombres() + " " + c.getValue().getCliente().getApellidos()));
        colColaborador.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getColaborador().getNombres() + " " + c.getValue().getColaborador().getApellidos()));
        colMetodoPago.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMetodoPago().toString()));
        colTotal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTotal()));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstado().toString()));
    }

    private void actualizarBotones(Venta v) {
        btnFactura.setDisable(v == null);
        btnAnular.setDisable(v == null || v.getEstado() == EstadoVenta.ANULADA);
    }

    private void configurarFiltro() {
        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> {
            String filtro = (nuevo == null) ? "" : nuevo.toLowerCase().trim();
            listaFiltrada.setPredicate(v -> {
                if (filtro.isEmpty()) return true;
                String cliente = v.getCliente() == null ? "consumidor final"
                        : (v.getCliente().getNombres() + " " + v.getCliente().getApellidos()).toLowerCase();
                String colaborador = (v.getColaborador().getNombres() + " " + v.getColaborador().getApellidos()).toLowerCase();
                return v.getNumeroFactura().toLowerCase().contains(filtro) || cliente.contains(filtro)
                        || colaborador.contains(filtro) || v.getEstado().toString().toLowerCase().contains(filtro);
            });
        });
    }

    private void cargarVentas() {
        tblVentas.setPlaceholder(new Label("Cargando ventas..."));
        btnRefrescar.setDisable(true);
        Async.ejecutar(ventaService::obtenerTodas, lista -> {
            listaVentas.setAll(lista);
            tblVentas.setPlaceholder(new Label("No hay ventas registradas."));
            btnRefrescar.setDisable(false);
        }, error -> {
            tblVentas.setPlaceholder(new Label("Error al cargar las ventas."));
            btnRefrescar.setDisable(false);
            Async.mensaje(error);
        });
    }

    private void abrirNuevaVenta() {
        try {
            Vistas.Vista<NuevaVentaController> vista = Vistas.cargar(Vistas.NUEVA_VENTA);
            Vistas.mostrarModal(vista, "BunBun Hub - Nueva Venta", btnNuevaVenta.getScene().getWindow());
            cargarVentas();
        } catch (IOException e) {
            e.printStackTrace();
            Ui.error("Error", "No se pudo abrir el formulario de venta.");
        }
    }

    private void anularVenta() {
        Venta v = tblVentas.getSelectionModel().getSelectedItem();
        if (v == null) return;
        if (!Ui.confirmar("Anular venta", "Se anulará la factura " + v.getNumeroFactura()
                + " y las existencias volverán al inventario. ¿Continuar?")) return;

        Async.ejecutarSinResultado(() -> ventaService.anularVenta(v.getId()), this::cargarVentas,
                error -> Ui.error("Error", Async.mensaje(error)));
    }

    /** Genera la factura en PDF con JasperReports. Reutilizado por NuevaVentaController. */
    static void exportarFactura(Window owner, Long idVenta) {
        Path destino = Ui.elegirDestinoPdf(owner, "factura-" + idVenta + ".pdf");
        if (destino == null) return;

        Async.ejecutarSinResultado(() -> {
                    Venta completa = new VentaService().obtenerCompleta(idVenta);
                    new ReporteService().generarFactura(completa, destino);
                }, () -> Ui.abrir(destino),
                error -> Ui.error("Error", "No se pudo generar la factura: " + Async.mensaje(error)));
    }
}