package ni.edu.uam.proyectofinalpae.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ni.edu.uam.proyectofinalpae.model.dto.HistorialTasasResponse;
import ni.edu.uam.proyectofinalpae.model.dto.TasaActualResponse;
import ni.edu.uam.proyectofinalpae.util.NegocioException;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;

public class TipoCambioService {
    private static final String BASE_URL = "https://api.frankfurter.dev/v1";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TipoCambioService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Endpoint 1 (Operación de detalle): GET /v1/latest?base={base}&symbols={destino}
     */
    public TasaActualResponse consultarTasaActual(String monedaBase, String monedaDestino) {
        String baseCod = URLEncoder.encode(monedaBase.trim().toUpperCase(), StandardCharsets.UTF_8);
        String destCod = URLEncoder.encode(monedaDestino.trim().toUpperCase(), StandardCharsets.UTF_8);
        String url = String.format("%s/latest?base=%s&symbols=%s", BASE_URL, baseCod, destCod);

        String json = ejecutarGet(url);
        try {
            TasaActualResponse respuesta = objectMapper.readValue(json, TasaActualResponse.class);
            if (respuesta.getRates() == null || respuesta.getRates().isEmpty()) {
                throw new NegocioException("La API no devolvió tasas para las divisas seleccionadas.");
            }
            return respuesta;
        } catch (NegocioException e) {
            throw e;
        } catch (Exception e) {
            throw new NegocioException("Error al procesar el JSON de tasa actual: " + e.getMessage());
        }
    }

    /**
     * Endpoint 2 (Consulta con filtros): GET /v1/{desde}..{hasta}?base={base}&symbols={destino}
     */
    public HistorialTasasResponse consultarHistorial(LocalDate desde, LocalDate hasta, String monedaBase, String monedaDestino) {
        String baseCod = URLEncoder.encode(monedaBase.trim().toUpperCase(), StandardCharsets.UTF_8);
        String destCod = URLEncoder.encode(monedaDestino.trim().toUpperCase(), StandardCharsets.UTF_8);
        String url = String.format("%s/%s..%s?base=%s&symbols=%s", BASE_URL, desde, hasta, baseCod, destCod);

        String json = ejecutarGet(url);
        try {
            HistorialTasasResponse respuesta = objectMapper.readValue(json, HistorialTasasResponse.class);
            if (respuesta.getRates() == null || respuesta.getRates().isEmpty()) {
                throw new NegocioException("No se encontraron registros históricos en el rango de fechas indicado.");
            }
            return respuesta;
        } catch (NegocioException e) {
            throw e;
        } catch (Exception e) {
            throw new NegocioException("Error al procesar el JSON histórico: " + e.getMessage());
        }
    }

    private String ejecutarGet(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status == 200) {
                return response.body();
            } else if (status == 404 || status == 422) {
                throw new NegocioException("Parámetros inválidos o sin datos disponibles (HTTP " + status + ").");
            } else {
                throw new NegocioException("El servicio externo respondió con error HTTP " + status + ".");
            }
        } catch (IOException e) {
            throw new NegocioException("Error de conexión con el servicio de divisas. Verifique su acceso a internet.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NegocioException("La consulta fue interrumpida.");
        }
    }
}