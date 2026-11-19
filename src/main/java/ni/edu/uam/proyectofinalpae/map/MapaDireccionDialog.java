package ni.edu.uam.proyectofinalpae.map;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Selector visual de ubicación OpenStreetMap: clic en el mapa, luego Guardar. */
public final class MapaDireccionDialog {
    private MapaDireccionDialog() {}
    public record Coordenadas(BigDecimal latitud, BigDecimal longitud) {}

    public static Optional<Coordenadas> seleccionar(Window padre, BigDecimal lat, BigDecimal lon) {
        Stage ventana = new Stage();
        ventana.setTitle("Seleccionar dirección - OpenStreetMap");
        if (padre != null) ventana.initOwner(padre);
        ventana.initModality(Modality.APPLICATION_MODAL);
        WebView mapa = new WebView();
        Label etiqueta = new Label("Haz clic en el mapa para elegir la ubicación.");
        Button guardar = new Button("Guardar ubicación");
        Button cancelar = new Button("Cancelar");
        guardar.setDisable(true);
        AtomicReference<Coordenadas> elegido = new AtomicReference<>();
        AtomicReference<Coordenadas> resultado = new AtomicReference<>();
        // Identifica la aplicación ante el proveedor de mapas.
        String contacto = System.getenv().getOrDefault("OSM_CONTACT", "proyecto-academico");
        mapa.getEngine().setUserAgent("BunBunHub-PAE/1.0 (" + contacto + ")");
        mapa.getEngine().setOnAlert(evento -> {
            String dato = evento.getData();
            if (!dato.startsWith("COORD:")) return;
            try {
                String[] parts = dato.substring(6).split(",");
                Coordenadas c = new Coordenadas(new BigDecimal(parts[0]), new BigDecimal(parts[1]));
                if (c.latitud().abs().compareTo(new BigDecimal("90")) > 0 ||
                        c.longitud().abs().compareTo(new BigDecimal("180")) > 0) return;
                elegido.set(c);
                etiqueta.setText("Lat: " + c.latitud() + "  |  Lon: " + c.longitud());
                guardar.setDisable(false);
            } catch (RuntimeException ignored) { etiqueta.setText("Coordenadas inválidas."); }
        });
        double inicialLat = lat == null ? 12.1364 : lat.doubleValue();
        double inicialLon = lon == null ? -86.2514 : lon.doubleValue();
        String tileUrl = System.getProperty("OSM_TILE_URL",
                System.getenv().getOrDefault("OSM_TILE_URL", "https://tile.openstreetmap.org/{z}/{x}/{y}.png"));
        String html = """
                <!DOCTYPE html><html lang="es"><head><meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
                <style>html,body,#map{width:100%%;height:100%%;margin:0;padding:0}</style></head>
                <body><div id="map"></div>
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <script>
                const map=L.map('map').setView([%s,%s],13);
                L.tileLayer('%s',{
                    attribution:'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>',
                    maxZoom:19}).addTo(map);
                let pin;
                map.on('click', e => {
                    const a=e.latlng.lat.toFixed(6), b=e.latlng.lng.toFixed(6);
                    if(!pin) pin=L.marker([a,b]).addTo(map); else pin.setLatLng([a,b]);
                    alert('COORD:'+a+','+b);
                });
                </script></body></html>
                """.formatted(inicialLat, inicialLon, tileUrl.replace("'", "%27"));
        // El CSS contiene 100%% para escapar el formateador anterior.
        html = html.replace("100%%", "100%");
        mapa.getEngine().loadContent(html);
        guardar.setOnAction(e -> { resultado.set(elegido.get()); ventana.close(); });
        cancelar.setOnAction(e -> ventana.close());
        HBox botones = new HBox(12, etiqueta, guardar, cancelar);
        botones.setStyle("-fx-padding: 10; -fx-alignment: center-left;");
        BorderPane contenido = new BorderPane(mapa); contenido.setBottom(botones);
        ventana.setScene(new Scene(contenido, 900, 650));
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
        String html = """
                <!DOCTYPE html><html lang="es"><head><meta charset="UTF-8">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
                <style>html,body,#map{width:100%%;height:100%%;margin:0}</style></head>
                <body><div id="map"></div>
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <script>
                const map=L.map('map').setView([%s,%s],16);
                L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                   attribution:'&copy; OpenStreetMap contributors',maxZoom:19
                }).addTo(map);
                L.marker([%s,%s]).addTo(map);
                </script></body></html>
                """.formatted(lat, lon, lat, lon);
        web.getEngine().loadContent(html);
        Label texto = new Label((direccion == null ? "" : direccion) + " | " + lat + ", " + lon);
        texto.setWrapText(true);
        Button cerrar = new Button("Cerrar");
        cerrar.setOnAction(e -> ventana.close());
        HBox pie = new HBox(12, texto, cerrar);
        pie.setStyle("-fx-padding: 10; -fx-alignment: center-left;");
        BorderPane panel = new BorderPane(web);
        panel.setBottom(pie);
        ventana.setScene(new Scene(panel, 850, 550));
        ventana.showAndWait();
    }
}
