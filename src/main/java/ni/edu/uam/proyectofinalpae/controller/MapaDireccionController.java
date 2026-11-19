package ni.edu.uam.proyectofinalpae.controller;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.Window;
import ni.edu.uam.proyectofinalpae.service.GeocodingService;
import ni.edu.uam.proyectofinalpae.util.Async;
import ni.edu.uam.proyectofinalpae.util.Ui;
import ni.edu.uam.proyectofinalpae.util.Vistas;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public class MapaDireccionController {
    public record Ubicacion(BigDecimal latitud, BigDecimal longitud) {}

    private static final String HTML = """
            <!DOCTYPE html><html><head><meta charset="utf-8">
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>html,body,#map{height:100%;margin:0}</style></head>
            <body><div id="map"></div>
            <script>
            var map = L.map('map').setView([LAT0, LON0], ZOOM0);
            L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                {maxZoom: 19, attribution: '&copy; OpenStreetMap contributors'}).addTo(map);
            var marcador = null;
            window.lat = null; window.lng = null;
            var editable = EDIT0;
            function colocar(lat, lng, zoom) {
                window.lat = lat; window.lng = lng;
                if (marcador) { marcador.setLatLng([lat, lng]); }
                else { marcador = L.marker([lat, lng]).addTo(map); }
                map.setView([lat, lng], zoom || map.getZoom());
            }
            map.on('click', function(e) { if (editable) { colocar(e.latlng.lat, e.latlng.lng); } });
            INICIAL
            </script></body></html>
            """;

    @FXML private TextField txtBusqueda;
    @FXML private Button btnBuscar, btnConfirmar, btnCancelar;
    @FXML private WebView wvMapa;
    @FXML private Label lblCoordenadas, lblMensaje;

    private final GeocodingService geocodingService = new GeocodingService();
    private WebEngine engine;
    private boolean paginaCargada;
    private String scriptPendiente;
    private Ubicacion resultado;

    /** Abre el mapa para elegir un punto. Vacío si el usuario cancela. */
    public static Optional<Ubicacion> seleccionar(Window owner, String textoInicial) {
        return abrir(owner, "BunBun Hub - Ubicar dirección", textoInicial, null, null, false);
    }

    /** Abre el mapa en modo lectura. Si no hay coordenadas guardadas, busca el texto en OSM. */
    public static void ver(Window owner, String texto, BigDecimal latitud, BigDecimal longitud) {
        abrir(owner, "BunBun Hub - Dirección", texto, latitud, longitud, true);
    }

    private static Optional<Ubicacion> abrir(Window owner, String titulo, String texto,
                                             BigDecimal lat, BigDecimal lon, boolean soloLectura) {
        try {
            Vistas.Vista<MapaDireccionController> vista = Vistas.cargar(Vistas.MAPA_DIRECCION);
            vista.controller().configurar(texto, lat, lon, soloLectura);
            Vistas.mostrarModal(vista, titulo, owner);
            return Optional.ofNullable(vista.controller().resultado);
        } catch (IOException e) {
            e.printStackTrace();
            Ui.error("Error", "No se pudo abrir el mapa.");
            return Optional.empty();
        }
    }

    @FXML
    public void initialize() {
        lblMensaje.setText("");
        engine = wvMapa.getEngine();
        engine.getLoadWorker().stateProperty().addListener((obs, anterior, estado) -> {
            if (estado == Worker.State.SUCCEEDED) {
                paginaCargada = true;
                if (scriptPendiente != null) {
                    engine.executeScript(scriptPendiente);
                    scriptPendiente = null;
                }
            }
        });
        btnBuscar.setOnAction(e -> buscar());
        txtBusqueda.setOnAction(e -> buscar());
        btnConfirmar.setOnAction(e -> confirmar());
        btnCancelar.setOnAction(e -> cerrar());
    }

    private void configurar(String texto, BigDecimal lat, BigDecimal lon, boolean soloLectura) {
        txtBusqueda.setText(texto == null ? "" : texto);
        btnConfirmar.setVisible(!soloLectura);
        btnConfirmar.setManaged(!soloLectura);
        lblCoordenadas.setText(soloLectura
                ? "Ubicación de la dirección."
                : "Busca la dirección o haz clic en el mapa para marcar el punto exacto.");

        boolean hayCoordenadas = lat != null && lon != null;
        String html = HTML
                .replace("LAT0", hayCoordenadas ? lat.toPlainString() : "12.1364")   // Managua por defecto
                .replace("LON0", hayCoordenadas ? lon.toPlainString() : "-86.2514")
                .replace("ZOOM0", hayCoordenadas ? "17" : "12")
                .replace("EDIT0", String.valueOf(!soloLectura))
                .replace("INICIAL", hayCoordenadas ? "colocar(" + lat.toPlainString() + "," + lon.toPlainString() + ",17);" : "");
        engine.loadContent(html);

        if (!hayCoordenadas && texto != null && !texto.isBlank()) buscar();
    }

    private void buscar() {
        String texto = txtBusqueda.getText().trim();
        if (texto.isEmpty()) {
            lblMensaje.setText("Escribe una dirección para buscar.");
            return;
        }
        lblMensaje.setText("Buscando...");
        btnBuscar.setDisable(true);
        Async.ejecutar(() -> geocodingService.buscar(texto), encontrada -> {
            btnBuscar.setDisable(false);
            if (encontrada.isPresent()) {
                var c = encontrada.get();
                ejecutarJs("colocar(" + c.latitud() + "," + c.longitud() + ",17);");
                lblMensaje.setText("Ajusta el punto con un clic si no es exacto.");
            } else {
                lblMensaje.setText("No se encontró esa dirección. Prueba con otro texto o marca el punto en el mapa.");
            }
        }, e -> {
            btnBuscar.setDisable(false);
            lblMensaje.setText(Async.mensaje(e));
        });
    }

    private void ejecutarJs(String js) {
        if (paginaCargada) engine.executeScript(js);
        else scriptPendiente = js;
    }

    private void confirmar() {
        Object lat = paginaCargada ? engine.executeScript("window.lat") : null;
        Object lng = paginaCargada ? engine.executeScript("window.lng") : null;
        if (!(lat instanceof Number) || !(lng instanceof Number)) {
            lblMensaje.setText("Busca una dirección o haz clic en el mapa para marcar el punto.");
            return;
        }
        resultado = new Ubicacion(escala(((Number) lat).doubleValue()), escala(((Number) lng).doubleValue()));
        cerrar();
    }

    private BigDecimal escala(double valor) {
        return BigDecimal.valueOf(valor).setScale(6, RoundingMode.HALF_UP);
    }

    private void cerrar() {
        ((Stage) btnCancelar.getScene().getWindow()).close();
    }
}