package ni.edu.uam.proyectofinalpae.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ni.edu.uam.proyectofinalpae.model.EstadoVenta;
import ni.edu.uam.proyectofinalpae.model.Venta;
import ni.edu.uam.proyectofinalpae.report.FilaInforme;
import ni.edu.uam.proyectofinalpae.report.ReporteService;
import ni.edu.uam.proyectofinalpae.service.DetalleVentaService;
import ni.edu.uam.proyectofinalpae.service.VentaService;
import ni.edu.uam.proyectofinalpae.util.Async;
import ni.edu.uam.proyectofinalpae.util.NegocioException;
import ni.edu.uam.proyectofinalpae.util.Ui;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Informes (informes-view.fxml). Antes esta lógica estaba mezclada en MainAdminController.
 * Muestra el resultado en pantalla y lo exporta a PDF con JasperReports.
 */
public class InformesController {
    private static final String BENEFICIOS = "Beneficios";
    private static final String DEMANDA = "Demanda de Productos";
    private static final String CLIENTES = "Mejores Clientes";

    @FXML private DatePicker dpFechaInicio;
    @FXML private DatePicker dpFechaFinal;
    @FXML private ComboBox<String> cbTipoInforme;
    @FXML private Button btnGenerar;        // NUEVO (antes era el método generarInforme del main)
    @FXML private Button btnExportarPdf;    // NUEVO: Jasper
    @FXML private Label lblMensaje;         // NUEVO

    @FXML private TableView<FilaInforme> tblInformes;
    @FXML private TableColumn<FilaInforme, String> colConcepto;
    @FXML private TableColumn<FilaInforme, String> colValor;

    private final VentaService ventaService = new VentaService();
    private final DetalleVentaService detalleVentaService = new DetalleVentaService();
    private final ReporteService reporteService = new ReporteService();
    private final ObservableList<FilaInforme> filas = FXCollections.observableArrayList();

    // Parámetros del último informe generado (el PDF exporta lo que se ve en pantalla)
    private String tituloActual;
    private LocalDate desdeActual;
    private LocalDate hastaActual;

    @FXML
    public void initialize() {
        lblMensaje.setText("");
        cbTipoInforme.setItems(FXCollections.observableArrayList(BENEFICIOS, DEMANDA, CLIENTES));
        dpFechaFinal.setValue(LocalDate.now());
        dpFechaInicio.setValue(LocalDate.now().minusMonths(1));

        colConcepto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getConcepto()));
        colValor.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getValor()));
        tblInformes.setItems(filas);
        tblInformes.setPlaceholder(new Label("Seleccione el periodo y el tipo de informe."));

        btnExportarPdf.setDisable(true);
        btnGenerar.setOnAction(e -> generarInforme());
        btnExportarPdf.setOnAction(e -> exportarPdf());
    }

    private void generarInforme() {
        lblMensaje.setText("");
        LocalDate desde = dpFechaInicio.getValue();
        LocalDate hasta = dpFechaFinal.getValue();
        String tipo = cbTipoInforme.getValue();

        if (desde == null || hasta == null || tipo == null) {
            lblMensaje.setText("Debe seleccionar las fechas y el tipo de informe.");
            return;
        }
        if (desde.isAfter(hasta)) {
            lblMensaje.setText("La fecha inicial no puede ser posterior a la final.");
            return;
        }

        btnGenerar.setDisable(true);
        Async.ejecutar(() -> calcular(tipo, desde, hasta), resultado -> {
            btnGenerar.setDisable(false);
            filas.setAll(resultado);
            tituloActual = tipo;
            desdeActual = desde;
            hastaActual = hasta;
            btnExportarPdf.setDisable(resultado.isEmpty());
            tblInformes.setPlaceholder(new Label("No hay datos en el periodo seleccionado."));
        }, error -> {
            btnGenerar.setDisable(false);
            lblMensaje.setText(Async.mensaje(error));
        });
    }

    /** Corre en segundo plano. */
    private List<FilaInforme> calcular(String tipo, LocalDate desde, LocalDate hasta) {
        List<FilaInforme> resultado = new ArrayList<>();
        switch (tipo) {
            case BENEFICIOS -> {
                List<Venta> ventas = ventaService.obtenerPorRango(desde, hasta).stream()
                        .filter(v -> v.getEstado() == EstadoVenta.COMPLETADA).toList();
                if (ventas.isEmpty()) break;
                resultado.add(new FilaInforme("Ventas completadas", String.valueOf(ventas.size())));
                resultado.add(new FilaInforme("Subtotal", dinero(sumar(ventas, Venta::getSubtotal))));
                resultado.add(new FilaInforme("IVA (15%)", dinero(sumar(ventas, Venta::getImpuesto))));
                resultado.add(new FilaInforme("Total de ingresos", dinero(sumar(ventas, Venta::getTotal))));
            }
            case DEMANDA -> detalleVentaService.productosMasVendidos(desde, hasta, 20)
                    .forEach(f -> resultado.add(new FilaInforme(String.valueOf(f[0]), f[1] + " unidades")));
            case CLIENTES -> ventaService.mejoresClientes(desde, hasta, 10)
                    .forEach(f -> resultado.add(new FilaInforme(f[0] + " " + f[1], dinero((BigDecimal) f[2]))));
            default -> throw new NegocioException("Tipo de informe no soportado.");
        }
        return resultado;
    }

    private void exportarPdf() {
        if (filas.isEmpty() || tituloActual == null) return;
        Path destino = Ui.elegirDestinoPdf(btnExportarPdf.getScene().getWindow(),
                "informe-" + tituloActual.toLowerCase().replace(' ', '-') + ".pdf");
        if (destino == null) return;

        List<FilaInforme> copia = new ArrayList<>(filas);
        Async.ejecutarSinResultado(
                () -> reporteService.generarInforme(tituloActual, desdeActual, hastaActual, copia, destino),
                () -> Ui.abrir(destino),
                error -> Ui.error("Error", "No se pudo generar el PDF: " + Async.mensaje(error)));
    }

    private BigDecimal sumar(List<Venta> ventas, Function<Venta, BigDecimal> campo) {
        return ventas.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String dinero(BigDecimal monto) {
        return "$" + monto.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}