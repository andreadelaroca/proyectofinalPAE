package ni.edu.uam.proyectofinalpae.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Fila genérica (concepto / valor) para la tabla de informes y para JasperReports. */
@Getter
@AllArgsConstructor
public class FilaInforme {
    private final String concepto;
    private final String valor;
}