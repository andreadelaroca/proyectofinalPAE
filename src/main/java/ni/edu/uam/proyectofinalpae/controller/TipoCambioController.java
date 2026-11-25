package ni.edu.uam.proyectofinalpae.controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ni.edu.uam.proyectofinalpae.model.dto.FilaTasaCambio;
import ni.edu.uam.proyectofinalpae.model.dto.HistorialTasasResponse;
import ni.edu.uam.proyectofinalpae.model.dto.TasaActualResponse;
import ni.edu.uam.proyectofinalpae.service.TipoCambioService;
import ni.edu.uam.proyectofinalpae.util.PestanaManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class TipoCambioController {

    @FXML private ComboBox<String> cbMonedaBase;
    @FXML private ComboBox<String> cbMonedaDestino;
    @FXML private TextField txtMonto;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;

    @FXML private Button btnConsultarActual;
    @FXML private Button btnConsultarHistorial;
    @FXML private Button btnCerrar;
    @FXML private ProgressIndicator piCargando;

    @FXML private Label lblResultadoActual;
    @FXML private Label lblEstado;

    @FXML private TableView<FilaTasaCambio> tblTasas;
    @FXML private TableColumn<FilaTasaCambio, String> colFecha;
    @FXML private TableColumn<FilaTasaCambio, String> colBase;
    @FXML private TableColumn<FilaTasaCambio, String> colDestino;
    @FXML private TableColumn<FilaTasaCambio, BigDecimal> colTasa;
    @FXML private TableColumn<FilaTasaCambio, BigDecimal> colConvertido;

    private final TipoCambioService tipoCambioService = new TipoCambioService();
    private final ObservableList<FilaTasaCambio> filasObservable = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        List<String> monedas = List.of("USD", "EUR", "MXN", "CAD", "GBP", "BRL", "JPY");
        cbMonedaBase.setItems(FXCollections.observableArrayList(monedas));
        cbMonedaDestino.setItems(FXCollections.observableArrayList(monedas));
        cbMonedaBase.setValue("USD");
        cbMonedaDestino.setValue("MXN");

        txtMonto.setText("100.00");
        dpDesde.setValue(LocalDate.now().minusDays(7));
        dpHasta.setValue(LocalDate.now());

        colFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().fecha()));
        colBase.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().monedaBase()));
        colDestino.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().monedaDestino()));
        colTasa.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().tasa()));
        colConvertido.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().montoConvertido()));
        tblTasas.setItems(filasObservable);

        btnConsultarActual.setOnAction(e -> ejecutarConsultaActual());
        btnConsultarHistorial.setOnAction(e -> ejecutarConsultaHistorial());
        btnCerrar.setOnAction(e -> PestanaManager.cerrar(btnCerrar));
    }

    private void ejecutarConsultaActual() {
        lblEstado.setText("");
        String base = cbMonedaBase.getValue();
        String destino = cbMonedaDestino.getValue();
        BigDecimal monto = validarEntradaBasica(base, destino);
        if (monto == null) return;

        setCargando(true);

        Task<TasaActualResponse> tarea = new Task<>() {
            @Override
            protected TasaActualResponse call() {
                return tipoCambioService.consultarTasaActual(base, destino);
            }
        };

        tarea.setOnSucceeded(e -> {
            setCargando(false);
            TasaActualResponse res = tarea.getValue();
            BigDecimal tasa = res.getRates().get(destino);
            BigDecimal convertido = monto.multiply(tasa).setScale(2, RoundingMode.HALF_UP);

            lblResultadoActual.setText(String.format(
                    "Fecha: %s | 1 %s = %s %s | Equivalente: %s %s",
                    res.getDate(), res.getBase(), tasa, destino, convertido, destino
            ));

            filasObservable.setAll(new FilaTasaCambio(res.getDate(), res.getBase(), destino, tasa, convertido));
        });

        tarea.setOnFailed(e -> {
            setCargando(false);
            lblEstado.setText(tarea.getException().getMessage());
        });

        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void ejecutarConsultaHistorial() {
        lblEstado.setText("");
        String base = cbMonedaBase.getValue();
        String destino = cbMonedaDestino.getValue();
        BigDecimal monto = validarEntradaBasica(base, destino);
        if (monto == null) return;

        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();

        if (desde == null || hasta == null) {
            lblEstado.setText("Seleccione ambas fechas para filtrar el historial.");
            return;
        }
        if (desde.isAfter(hasta)) {
            lblEstado.setText("La fecha inicial no puede ser posterior a la fecha final.");
            return;
        }
        if (hasta.isAfter(LocalDate.now())) {
            lblEstado.setText("La fecha final no puede ser futura.");
            return;
        }

        setCargando(true);

        Task<HistorialTasasResponse> tarea = new Task<>() {
            @Override
            protected HistorialTasasResponse call() {
                return tipoCambioService.consultarHistorial(desde, hasta, base, destino);
            }
        };

        tarea.setOnSucceeded(e -> {
            setCargando(false);
            HistorialTasasResponse res = tarea.getValue();
            List<FilaTasaCambio> nuevasFilas = new ArrayList<>();

            for (Map.Entry<String, Map<String, BigDecimal>> entrada : res.getRates().entrySet()) {
                BigDecimal tasa = entrada.getValue().get(destino);
                if (tasa != null) {
                    BigDecimal convertido = monto.multiply(tasa).setScale(2, RoundingMode.HALF_UP);
                    nuevasFilas.add(new FilaTasaCambio(entrada.getKey(), res.getBase(), destino, tasa, convertido));
                }
            }

            nuevasFilas.sort(Comparator.comparing(FilaTasaCambio::fecha).reversed());
            filasObservable.setAll(nuevasFilas);
            lblResultadoActual.setText(String.format("Historial cargado: %d registros (%s a %s)",
                    nuevasFilas.size(), res.getStartDate(), res.getEndDate()));
        });

        tarea.setOnFailed(e -> {
            setCargando(false);
            lblEstado.setText(tarea.getException().getMessage());
        });

        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    private BigDecimal validarEntradaBasica(String base, String destino) {
        if (base == null || destino == null) {
            lblEstado.setText("Seleccione la moneda base y la moneda destino.");
            return null;
        }
        if (base.equals(destino)) {
            lblEstado.setText("La moneda base y la moneda destino deben ser diferentes.");
            return null;
        }
        try {
            BigDecimal monto = new BigDecimal(txtMonto.getText().trim());
            if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                lblEstado.setText("El monto debe ser mayor a cero.");
                return null;
            }
            return monto;
        } catch (NumberFormatException ex) {
            lblEstado.setText("Ingrese un monto numérico válido (ej. 150.50).");
            return null;
        }
    }

    private void setCargando(boolean cargando) {
        piCargando.setVisible(cargando);
        btnConsultarActual.setDisable(cargando);
        btnConsultarHistorial.setDisable(cargando);
    }
}