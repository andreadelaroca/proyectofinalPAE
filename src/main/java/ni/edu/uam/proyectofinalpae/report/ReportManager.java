package ni.edu.uam.proyectofinalpae.report;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.view.JasperViewer;
import ni.edu.uam.proyectofinalpae.config.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;

import java.io.InputStream;
import java.util.Map;

public class ReportManager {
    public static void generarReporte(String reportPath, Map<String, Object> parametros) {
        try (Session session = HibernateUtil.factory().openSession()) {
            session.doReturningWork((ReturningWork<Void>) connection -> {
                try {
                    InputStream reportStream = ReportManager.class.getResourceAsStream("/" + reportPath);
                    if (reportStream == null) {
                        throw new JRException("No se encontró el archivo del reporte: " + reportPath);
                    }
                    JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

                    JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, connection);

                    JasperViewer.viewReport(jasperPrint, false);
                } catch (JRException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Error al generar el reporte: " + e.getMessage());
                }
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}