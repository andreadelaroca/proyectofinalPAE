package ni.edu.uam.proyectofinalpae.report;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import ni.edu.uam.proyectofinalpae.model.Venta;
import ni.edu.uam.proyectofinalpae.util.NegocioException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Genera PDFs con JasperReports. Las plantillas (.jrxml) van en:
 *   src/main/resources/ni/edu/uam/proyectofinalpae/reports/
 *   - factura.jrxml  (fuente de datos: DetalleVenta -> campos cantidad, precioUnitario, subtotal, producto.nombre, producto.codigo)
 *   - informe.jrxml  (fuente de datos: FilaInforme -> campos concepto, valor)
 * Bloquea el hilo: llamar siempre desde Async.
 */
public class ReporteService {
    private static final String RUTA = "/ni/edu/uam/proyectofinalpae/reports/";
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** La venta debe venir completa (VentaService.obtenerCompleta). */
    public void generarFactura(Venta venta, Path destino) {
        String cliente = venta.getCliente() == null
                ? "Consumidor final"
                : venta.getCliente().getNombres() + " " + venta.getCliente().getApellidos();

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("numeroFactura", venta.getNumeroFactura());
        parametros.put("fecha", venta.getFecha().format(FECHA_HORA));
        parametros.put("cliente", cliente);
        parametros.put("colaborador", venta.getColaborador().getNombres() + " " + venta.getColaborador().getApellidos());
        parametros.put("metodoPago", venta.getMetodoPago().toString());
        parametros.put("estado", venta.getEstado().toString());
        parametros.put("subtotal", venta.getSubtotal());
        parametros.put("impuesto", venta.getImpuesto());
        parametros.put("total", venta.getTotal());

        exportarPdf("factura.jrxml", parametros, venta.getDetalles(), destino);
    }

    public void generarInforme(String titulo, LocalDate desde, LocalDate hasta,
                               List<FilaInforme> filas, Path destino) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("titulo", titulo);
        parametros.put("desde", desde.format(FECHA));
        parametros.put("hasta", hasta.format(FECHA));
        exportarPdf("informe.jrxml", parametros, filas, destino);
    }

    public void exportarPdf(String plantilla, Map<String, Object> parametros, Collection<?> datos, Path destino) {
        try (InputStream in = ReporteService.class.getResourceAsStream(RUTA + plantilla)) {
            if (in == null) throw new NegocioException("No se encontró la plantilla de reporte " + plantilla + ".");
            JasperReport reporte = JasperCompileManager.compileReport(in);
            JasperPrint impresion = JasperFillManager.fillReport(reporte, parametros, new JRBeanCollectionDataSource(datos));
            JasperExportManager.exportReportToPdfFile(impresion, destino.toString());
        } catch (JRException | IOException e) {
            throw new RuntimeException("Error al generar el reporte " + plantilla, e);
        }
    }
}