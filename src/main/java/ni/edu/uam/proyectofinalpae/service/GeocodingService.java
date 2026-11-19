package ni.edu.uam.proyectofinalpae.service;

import ni.edu.uam.proyectofinalpae.util.NegocioException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convierte texto de dirección a coordenadas con Nominatim (OpenStreetMap).
 * Política de uso: máximo 1 petición por segundo y User-Agent identificable.
 * Bloquea el hilo: llamar siempre desde Async.
 */
public class GeocodingService {
    public record Coordenadas(double latitud, double longitud) {}

    private static final String PAIS = "ni"; // limita la búsqueda a Nicaragua; quítalo si necesitas otros países
    private static final Pattern LAT_LON =
            Pattern.compile("\"lat\"\\s*:\\s*\"(-?[0-9.]+)\"\\s*,\\s*\"lon\"\\s*:\\s*\"(-?[0-9.]+)\"");

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public Optional<Coordenadas> buscar(String texto) {
        if (texto == null || texto.isBlank()) return Optional.empty();
        try {
            String url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=" + PAIS
                    + "&q=" + URLEncoder.encode(texto.trim(), StandardCharsets.UTF_8);
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "BunBunHub/1.0 (proyecto academico UAM)")
                    .header("Accept-Language", "es")
                    .timeout(Duration.ofSeconds(15))
                    .GET().build();
            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() != 200) {
                throw new NegocioException("El servicio de mapas respondió con error " + respuesta.statusCode() + ".");
            }
            Matcher m = LAT_LON.matcher(respuesta.body());
            if (!m.find()) return Optional.empty();
            return Optional.of(new Coordenadas(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2))));
        } catch (NegocioException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NegocioException("Búsqueda interrumpida.");
        } catch (Exception e) {
            throw new NegocioException("No se pudo contactar el servicio de mapas. Revise su conexión a internet.");
        }
    }
}