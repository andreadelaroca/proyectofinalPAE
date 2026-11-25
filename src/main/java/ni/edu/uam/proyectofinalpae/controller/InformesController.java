package ni.edu.uam.proyectofinalpae.controller;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.proyectofinalpae.report.ReporteService;
import ni.edu.uam.proyectofinalpae.report.VistaPreviaReporte;
import ni.edu.uam.proyectofinalpae.report.ReporteService.FilaResumen;

import java.time.LocalDate;
import java.util.List;

public class InformesController {

    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private ComboBox<String> cbTipoInforme;
    @FXML private Button btnGenerarInforme;
    @FXML private TableView<FilaResumen> tblInformes;
    @FXML private TableColumn<FilaResumen, String> colConcepto;
    @FXML private TableColumn<FilaResumen, String> colValorTotal;

    private final ReporteService reporteService = new ReporteService();

    @FXML
    public void initialize() {
        colConcepto.setCellValueFactory(new PropertyValueFactory<>("concepto"));
        colValorTotal.setCellValueFactory(new PropertyValueFactory<>("valorTotal"));

        cbTipoInforme.setItems(FXCollections.observableArrayList("Pedidos", "Ventas"));
        cbTipoInforme.getSelectionModel().selectFirst();

        dpDesde.setValue(LocalDate.now().withDayOfMonth(1));
        dpHasta.setValue(LocalDate.now());

        // Actualiza la tabla resumen al cambiar filtros
        dpDesde.setOnAction(e -> cargarResumen());
        dpHasta.setOnAction(e -> cargarResumen());
        cbTipoInforme.setOnAction(e -> cargarResumen());

        btnGenerarInforme.setOnAction(e -> generarPdf());
        cargarResumen();
    }

    private void cargarResumen() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        String tipo = cbTipoInforme.getValue();
        if (desde == null || hasta == null || tipo == null || hasta.isBefore(desde)) return;

        Task<List<FilaResumen>> task = new Task<>() {
            @Override
            protected List<FilaResumen> call() {
                return reporteService.obtenerResumen(tipo, desde, hasta);
            }
        };
        task.setOnSucceeded(e -> tblInformes.setItems(FXCollections.observableArrayList(task.getValue())));
        new Thread(task).start();
    }

    private void generarPdf() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        String tipo = cbTipoInforme.getValue();

        if (desde == null || hasta == null || tipo == null || hasta.isBefore(desde)) {
            mostrarAlerta(Alert.AlertType.WARNING, "Datos inválidos", "Verifica el rango de fechas y el tipo de informe.");
            return;
        }

        String archivo = "informe_" + tipo.toLowerCase() + "_" + desde + "_" + hasta + ".pdf";
        // Primero la vista previa; el PDF se descarga desde ahí
        VistaPreviaReporte.generarYMostrar(btnGenerarInforme, "Informe de " + tipo, archivo, () ->
                "Pedidos".equals(tipo)
                        ? reporteService.generarPedidos(desde, hasta)
                        : reporteService.generarVentas(desde, hasta));
        cargarResumen();
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}