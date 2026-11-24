package ni.edu.uam.proyectofinalpae.controller;

import ni.edu.uam.proyectofinalpae.util.PestanaManager;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import ni.edu.uam.proyectofinalpae.model.EstadoVenta;
import ni.edu.uam.proyectofinalpae.model.Venta;
import ni.edu.uam.proyectofinalpae.report.ReporteService;
import ni.edu.uam.proyectofinalpae.service.VentaService;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class VentaController {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private TextField txtBuscar;
    @FXML private Button btnRefrescar, btnNuevaVenta, btnAnular, btnFactura;
    @FXML private TableView<Venta> tblVentas;
    @FXML private TableColumn<Venta, String> colNumeroFactura, colFecha, colCliente, colColaborador, colMetodoPago, colEstado;
    @FXML private TableColumn<Venta, BigDecimal> colTotal;

    private final VentaService ventaService = new VentaService();
    private final ObservableList<Venta> ventas = FXCollections.observableArrayList();
    private FilteredList<Venta> filtradas;

    @FXML public void initialize() {
        colNumeroFactura.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumeroFactura()));
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFecha() == null ? "" : c.getValue().getFecha().format(FORMATO)));
        colCliente.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCliente() == null ? "Consumidor final" : nombre(c.getValue().getCliente().getNombres(), c.getValue().getCliente().getApellidos())));
        colColaborador.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getColaborador() == null ? "" : nombre(c.getValue().getColaborador().getNombres(), c.getValue().getColaborador().getApellidos())));
        colMetodoPago.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getMetodoPago())));
        colTotal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTotal()));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getEstado())));

        filtradas = new FilteredList<>(ventas, v -> true);
        tblVentas.setItems(filtradas);
        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> {
            String q = nuevo == null ? "" : nuevo.trim().toLowerCase(Locale.ROOT);
            filtradas.setPredicate(v -> q.isBlank() ||
                    contiene(v.getNumeroFactura(), q) ||
                    contiene(v.getCliente() == null ? "consumidor final" : nombre(v.getCliente().getNombres(), v.getCliente().getApellidos()), q) ||
                    contiene(v.getColaborador() == null ? "" : nombre(v.getColaborador().getNombres(), v.getColaborador().getApellidos()), q) ||
                    contiene(String.valueOf(v.getEstado()), q));
        });
        tblVentas.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> actualizarBotones(actual));
        actualizarBotones(null);
        btnRefrescar.setOnAction(e -> cargarVentas());
        btnNuevaVenta.setOnAction(e -> abrirNuevaVenta());
        btnAnular.setOnAction(e -> anularVenta());
        btnFactura.setOnAction(e -> {
            Venta seleccionada = tblVentas.getSelectionModel().getSelectedItem();
            if (seleccionada != null) exportarFactura(btnFactura.getScene().getWindow(), seleccionada.getId());
        });
        cargarVentas();
    }

    private static String nombre(String nombres, String apellidos) {
        return ((nombres == null ? "" : nombres) + " " + (apellidos == null ? "" : apellidos)).trim();
    }
    private static boolean contiene(String texto, String q) {
        return texto != null && texto.toLowerCase(Locale.ROOT).contains(q);
    }
    private void actualizarBotones(Venta v) {
        btnFactura.setDisable(v == null);
        btnAnular.setDisable(v == null || v.getEstado() == EstadoVenta.ANULADA);
    }
    private void cargarVentas() {
        try {
            List<Venta> resultado = ventaService.obtenerTodas();
            ventas.setAll(resultado);
            tblVentas.setPlaceholder(new Label("No hay ventas registradas."));
        } catch (Exception ex) {
            tblVentas.setPlaceholder(new Label("Error al cargar las ventas."));
            error("Error de ventas", ex);
        }
    }
    private void abrirNuevaVenta() {
        try {
            var url = getClass().getResource("/ni/edu/uam/proyectofinalpae/fxml/nueva-venta-view.fxml");
            if (url == null) throw new IOException("Falta nueva-venta-view.fxml");
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            btnNuevaVenta.setDisable(true);
            PestanaManager.abrir(root, "Nueva venta", btnNuevaVenta, () -> {
                btnNuevaVenta.setDisable(false);
                cargarVentas();
            });
        } catch (Exception ex) { error("No se pudo abrir Nueva venta", ex); }
    }
    private void anularVenta() {
        Venta v = tblVentas.getSelectionModel().getSelectedItem();
        if (v == null) return;
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "Se anulará " + v.getNumeroFactura() + ". Si corresponde, se devolverán existencias al inventario. ¿Continuar?",
                ButtonType.YES, ButtonType.NO);
        confirmacion.setHeaderText("Confirmar anulación");
        if (confirmacion.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try { ventaService.anularVenta(v.getId()); cargarVentas(); }
        catch (Exception ex) { error("No se pudo anular la venta", ex); }
    }
    public static void exportarFactura(Window owner, Long idVenta) {
        if (idVenta == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar factura PDF");
        chooser.setInitialFileName("factura-" + idVenta + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));
        File destino = chooser.showSaveDialog(owner);
        if (destino == null) return;
        try {
            Venta completa = new VentaService().obtenerCompleta(idVenta);
            Path pdf = new ReporteService().generarFactura(completa, destino.toPath());
            new Alert(Alert.AlertType.INFORMATION, "Factura generada en: " + pdf.toAbsolutePath()).showAndWait();
        } catch (Exception ex) { error("Error al generar factura", ex); }
    }
    private static void error(String titulo, Exception ex) {
        new Alert(Alert.AlertType.ERROR, ex.getMessage() == null ? ex.toString() : ex.getMessage(), ButtonType.OK) {{
            setTitle(titulo); setHeaderText(null);
        }}.showAndWait();
    }
}
