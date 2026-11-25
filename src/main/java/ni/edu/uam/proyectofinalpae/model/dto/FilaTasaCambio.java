package ni.edu.uam.proyectofinalpae.model.dto;

import java.math.BigDecimal;

public record FilaTasaCambio(
        String fecha,
        String monedaBase,
        String monedaDestino,
        BigDecimal tasa,
        BigDecimal montoConvertido
) {}