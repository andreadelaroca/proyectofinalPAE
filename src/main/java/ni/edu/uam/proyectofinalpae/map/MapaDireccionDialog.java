package ni.edu.uam.proyectofinalpae.map;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import ni.edu.uam.proyectofinalpae.util.SceneManager;

/** Selector visual de ubicación OpenStreetMap (Leaflet): clic en el mapa, luego Guardar. */
public final class MapaDireccionDialog {
    private static final String TILE_POR_DEFECTO = "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
    private static final BigDecimal LAT_DEFECTO = new BigDecimal("12.1364");
    private static final BigDecimal LON_DEFECTO = new BigDecimal("-86.2514");

    private static final String HTML_BASE = """
            <!DOCTYPE html><html lang="es"><head><meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
            <style>
              html,body{width:100%;height:100%;margin:0;padding:0;overflow:hidden;background:#ddd}
              #map{position:absolute;top:0;right:0;bottom:0;left:0}
            </style></head>
            <body><div id="map"></div>
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <script>
            // El WebView de JavaFX renderiza mal los translate3d: forzar posicionamiento 2D.
            L.Browser.any3d = false;
            L.Icon.Default.imagePath = 'https://unpkg.com/leaflet@1.9.4/dist/images/';

            const map = L.map('map', {
                zoomAnimation: true, fadeAnimation: true, markerZoomAnimation: false,
                inertia: true, zoomControl: true
            }).setView([__LAT__, __LON__], __ZOOM__);

            const capa = L.tileLayer('__TILES__', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>',
                maxZoom: 19,
                updateWhenIdle: false,
                updateWhenZooming: false,
                keepBuffer: 4
            }).addTo(map);

            // Reintenta tiles que fallaron (límite de conexiones del WebView).
            capa.on('tileerror', ev => {
                const t = ev.tile;
                t._retries = (t._retries || 0) + 1;
                if (t._retries <= 3) setTimeout(() => { t.src = ev.tile.src.split('?')[0] + '?r=' + t._retries; }, 500 * t._retries);
            });

            let pin = __PIN__ ? L.marker([__LAT__, __LON__]).addTo(map) : null;

            if (__CLICK__) {
                map.on('click', e => {
                    const a = e.latlng.lat.toFixed(6), b = e.latlng.lng.toFixed(6);
                    if (!pin) pin = L.marker([a, b]).addTo(map); else pin.setLatLng([a, b]);
                    alert('COORD:' + a + ',' + b);
                });
            }

            function ajustar() { map.invalidateSize(false); }
            window.addEventListener('resize', ajustar);
            setTimeout(ajustar, 100); setTimeout(ajustar, 500);
            </script></body></html>
            """;

    private MapaDireccionDialog() {}
    public record Coordenadas(BigDecimal latitud, BigDecimal longitud) {}

    private static String tileUrl() {
        return System.getProperty("OSM_TILE_URL",
                System.getenv().getOrDefault("OSM_TILE_URL", TILE_POR_DEFECTO));
    }

    private static String construirHtml(BigDecimal lat, BigDecimal lon, int zoom,
                                        boolean marcador, boolean seleccionable) {
        return HTML_BASE
                .replace("__LAT__", lat.toPlainString())
                .replace("__LON__", lon.toPlainString())
                .replace("__ZOOM__", String.valueOf(zoom))
                .replace("__TILES__", tileUrl().replace("'", "%27"))
                .replace("__PIN__", String.valueOf(marcador))
                .replace("__CLICK__", String.valueOf(seleccionable));
    }

    /** Pide a Leaflet recalcular el tamaño cuando cambia la ventana. */
    private static void enlazarRedimension(Stage ventana, WebView web) {
        WebEngine engine = web.getEngine();
        Runnable ajustar = () -> Platform.runLater(() -> {
            if (engine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
                try { engine.executeScript("if(window.map){map.invalidateSize(false)}"); }
                catch (RuntimeException ignored) { }
            }
        });
        ventana.widthProperty().addListener((o, a, b) -> ajustar.run());
        ventana.heightProperty().addListener((o, a, b) -> ajustar.run());
        ventana.maximizedProperty().addListener((o, a, b) -> ajustar.run());
        ventana.fullScreenProperty().addListener((o, a, b) -> ajustar.run());
        engine.getLoadWorker().stateProperty().addListener((o, a, estado) -> {
            if (estado == Worker.State.SUCCEEDED) ajustar.run();
        });
    }

    public static Optional<Coordenadas> seleccionar(Window padre, BigDecimal lat, BigDecimal lon) {
        Stage ventana = new Stage();
        ventana.setTitle("Seleccionar dirección - OpenStreetMap");
        if (padre != null) ventana.initOwner(padre);
        ventana.initModality(Modality.APPLICATION_MODAL);

        WebView mapa = new WebView();
        mapa.setContextMenuEnabled(false);
        Label etiqueta = new Label("Haz clic en el mapa para elegir la ubicación.");
        Button guardar = new Button("Guardar ubicación");
        Button cancelar = new Button("Cancelar");
        guardar.setDisable(true);
        AtomicReference<Coordenadas> elegido = new AtomicReference<>();
        AtomicReference<Coordenadas> resultado = new AtomicReference<>();

        // Identifica la aplicación ante el proveedor de mapas (política de uso de OSM).
        String contacto = System.getenv().getOrDefault("OSM_CONTACT", "proyecto-academico");
        mapa.getEngine().setUserAgent("BunBunHub-PAE/1.0 (" + contacto + ")");
        mapa.getEngine().setOnAlert(evento -> {
            String dato = evento.getData();
            if (dato == null || !dato.startsWith("COORD:")) return;
            try {
                String[] parts = dato.substring(6).split(",");
                Coordenadas c = new Coordenadas(new BigDecimal(parts[0]), new BigDecimal(parts[1]));
                if (c.latitud().abs().compareTo(new BigDecimal("90")) > 0
                        || c.longitud().abs().compareTo(new BigDecimal("180")) > 0) return;
                elegido.set(c);
                etiqueta.setText("Lat: " + c.latitud() + "  |  Lon: " + c.longitud());
                guardar.setDisable(false);
            } catch (RuntimeException ex) { etiqueta.setText("Coordenadas inválidas."); }
        });

        boolean hayPunto = lat != null && lon != null;
        BigDecimal inicialLat = hayPunto ? lat : LAT_DEFECTO;
        BigDecimal inicialLon = hayPunto ? lon : LON_DEFECTO;
        mapa.getEngine().loadContent(construirHtml(inicialLat, inicialLon, hayPunto ? 16 : 13, hayPunto, true));

        guardar.setOnAction(e -> { resultado.set(elegido.get()); ventana.close(); });
        cancelar.setOnAction(e -> ventana.close());

        HBox.setHgrow(etiqueta, Priority.ALWAYS);
        etiqueta.setMaxWidth(Double.MAX_VALUE);
        HBox botones = new HBox(12, etiqueta, guardar, cancelar);
        botones.setStyle("-fx-padding: 10; -fx-alignment: center-left;");
        BorderPane contenido = new BorderPane(mapa);
        contenido.setBottom(botones);

        ventana.setScene(new Scene(contenido, 900, 650));
        enlazarRedimension(ventana, mapa);
        SceneManager.pantallaCompleta(ventana);
        ventana.showAndWait();
        return Optional.ofNullable(resultado.get());
    }

    /** Muestra las coordenadas guardadas sin permitir modificarlas. */
    public static void ver(Window padre, String direccion, BigDecimal lat, BigDecimal lon) {
        if (lat == null || lon == null) throw new IllegalArgumentException("Coordenadas no disponibles");
        Stage ventana = new Stage();
        ventana.setTitle("Dirección - OpenStreetMap");
        if (padre != null) ventana.initOwner(padre);
        ventana.initModality(Modality.WINDOW_MODAL);

        WebView web = new WebView();
        web.setContextMenuEnabled(false);
        String contacto = System.getenv().getOrDefault("OSM_CONTACT", "proyecto-academico");
        web.getEngine().setUserAgent("BunBunHub-PAE/1.0 (" + contacto + ")");
        web.getEngine().loadContent(construirHtml(lat, lon, 16, true, false));

        Label texto = new Label((direccion == null ? "" : direccion) + " | " + lat + ", " + lon);
    }
}