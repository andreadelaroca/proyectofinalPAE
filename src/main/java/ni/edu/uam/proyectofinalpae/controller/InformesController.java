package ni.edu.uam.proyectofinalpae.controller;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import ni.edu.uam.proyectofinalpae.report.ReporteService;
import ni.edu.uam.proyectofinalpae.report.ReporteService.FilaResumen;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Path;
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

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar informe PDF");
        chooser.setInitialFileName("informe_" + tipo.toLowerCase() + "_" + desde + "_" + hasta + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo PDF", "*.pdf"));

        File archivo = chooser.showSaveDialog(btnGenerarInforme.getScene().getWindow());
        if (archivo == null) return;

        btnGenerarInforme.setDisable(true);
        Path destino = archivo.toPath();

        Task<Path> task = new Task<>() {
            @Override
            protected Path call() throws Exception {
                if ("Pedidos".equals(tipo)) {
                    return reporteService.exportarPedidos(desde, hasta, destino);
                } else {
                    return reporteService.exportarVentas(desde, hasta, destino);
                }
            }
        };

        task.setOnSucceeded(e -> {
            btnGenerarInforme.setDisable(false);
            cargarResumen();
            mostrarAlerta(Alert.AlertType.INFORMATION, "Informe generado", "PDF guardado en:\n" + destino);
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(destino.toFile());
                }
            } catch (Exception ignored) {}
        });

        task.setOnFailed(e -> {
            btnGenerarInforme.setDisable(false);
            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error al generar PDF", ex != null ? ex.getMessage() : "Error desconocido");
        });

        new Thread(task).start();
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}