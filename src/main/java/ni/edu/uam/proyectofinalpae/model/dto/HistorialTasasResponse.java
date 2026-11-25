package ni.edu.uam.proyectofinalpae.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistorialTasasResponse {
    private String base;

    @JsonProperty("start_date")
    private String startDate;

    @JsonProperty("end_date")
    private String endDate;

    // Clave exterior: fecha ("2026-11-20"), Clave interior: moneda ("EUR"), Valor: tasa
    private Map<String, Map<String, BigDecimal>> rates;
}