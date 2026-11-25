package ni.edu.uam.proyectofinalpae.report;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import ni.edu.uam.proyectofinalpae.util.PestanaManager;

import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Vista previa de un reporte JasperReports dentro de JavaFX: muestra las páginas
 * renderizadas, permite navegar y hacer zoom, y solo entonces descargar el PDF.
 * Se abre como pestaña del panel (o ventana si no hay panel, p. ej. el cliente).
 */
public final class VistaPreviaReporte {

    /** Resolución con la que se renderiza cada página (2x = nítido al hacer zoom). */
    private static final float ESCALA_RENDER = 2f;
    private static final double[] ZOOMS = {0.5, 0.75, 1.0, 1.25, 1.5, 2.0};

    private final JasperPrint reporte;
    private final String nombreArchivo;
    private final Map<Integer, Image> cache = new HashMap<>();

    private final ImageView pagina = new ImageView();
    private final Label lblPagina = new Label();
    private final Label lblZoom = new Label();
    private final Button btnAnterior = new Button("◀");
    private final Button btnSiguiente = new Button("▶");
    private final Button btnDescargar = new Button("Descargar PDF");
    private final Button btnCerrar = new Button("Cerrar");
    private int indice = 0;
    private int zoom = 2; // 100 %

    private VistaPreviaReporte(JasperPrint reporte, String nombreArchivo) {
        this.reporte = reporte;
        this.nombreArchivo = nombreArchivo;
    }

    /**
     * Genera el reporte en segundo plano y abre su vista previa.
     * @param origen   control que lo solicita (se deshabilita mientras se genera)
     * @param titulo   texto de la pestaña / ventana
     * @param archivo  nombre sugerido al descargar (p. ej. "mis-pedidos.pdf")
     */
    public static void generarYMostrar(Control origen, String titulo, String archivo, Callable<JasperPrint> generador) {
        origen.setDisable(true);
        Task<JasperPrint> tarea = new Task<>() {
            @Override
            protected JasperPrint call() throws Exception {
                return generador.call();
            }
        };
        tarea.setOnSucceeded(e -> {
            origen.setDisable(false);
            mostrar(tarea.getValue(), titulo, archivo, origen);
        });
        tarea.setOnFailed(e -> {
            origen.setDisable(false);
            Throwable ex = tarea.getException();
            if (ex != null) ex.printStackTrace();
            alerta(Alert.AlertType.ERROR, "No se pudo generar el reporte",
                    ex == null ? "Error desconocido" : (ex.getMessage() == null ? ex.toString() : ex.getMessage()));
        });
        Thread hilo = new Thread(tarea, "generar-reporte");
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Abre la vista previa de un reporte ya generado. */
    public static void mostrar(JasperPrint reporte, String titulo, String archivo, Node origen) {
        VistaPreviaReporte vista = new VistaPreviaReporte(reporte, archivo);
        PestanaManager.abrir(vista.construir(titulo), "Vista previa: " + titulo, origen, null);
    }

    // ------------------------------------------------------------------ interfaz

    private BorderPane construir(String titulo) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("subtitulo-seccion");

        btnAnterior.getStyleClass().add("btn-secundario");
        btnSiguiente.getStyleClass().add("btn-secundario");
        Button btnMenos = new Button("−");
        Button btnMas = new Button("+");
        btnMenos.getStyleClass().add("btn-secundario");
        btnMas.getStyleClass().add("btn-secundario");
        btnDescargar.getStyleClass().add("btn-primario");
        btnCerrar.getStyleClass().add("btn-secundario");
        lblPagina.getStyleClass().add("label-campo");
        lblZoom.getStyleClass().add("label-campo");
        lblZoom.setMinWidth(48);
        lblZoom.setAlignment(Pos.CENTER);

        btnAnterior.setOnAction(e -> irA(indice - 1));
        btnSiguiente.setOnAction(e -> irA(indice + 1));
        btnMenos.setOnAction(e -> cambiarZoom(-1));
        btnMas.setOnAction(e -> cambiarZoom(1));
        btnDescargar.setOnAction(e -> descargar());
        btnCerrar.setOnAction(e -> PestanaManager.cerrar(btnCerrar));

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox barra = new HBox(10, lblTitulo, espacio,
                btnAnterior, lblPagina, btnSiguiente, separador(),
                btnMenos, lblZoom, btnMas, separador(),
                btnCerrar, btnDescargar);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(12, 20, 12, 20));
        barra.getStyleClass().add("barra-preview");

        // La página se muestra como una hoja con sombra sobre fondo gris azulado
        StackPane hoja = new StackPane(pagina);
        hoja.getStyleClass().add("hoja-preview");
        hoja.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane fondo = new StackPane(hoja);
        fondo.setPadding(new Insets(24));
        fondo.getStyleClass().add("fondo-preview");

        ScrollPane scroll = new ScrollPane(fondo);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setPannable(true);
        scroll.getStyleClass().add("fondo-preview");

        BorderPane raiz = new BorderPane(scroll);
        raiz.setTop(barra);
        raiz.setPrefSize(1000, 700);
        raiz.getStyleClass().add("llenar-pestana");

        if (reporte.getPages().isEmpty()) {
            fondo.getChildren().setAll(new Label("El reporte no tiene páginas."));
            btnDescargar.setDisable(true);
        } else {
            irA(0);
        }
        actualizarZoom();
        return raiz;
    }

    private static Separator separador() {
        Separator s = new Separator(javafx.geometry.Orientation.VERTICAL);
        s.setPrefHeight(26);
        return s;
    }

    private void irA(int nuevo) {
        int total = reporte.getPages().size();
        if (total == 0 || nuevo < 0 || nuevo >= total) return;
        indice = nuevo;
        lblPagina.setText("Página " + (indice + 1) + " de " + total);
        btnAnterior.setDisable(indice == 0);
        btnSiguiente.setDisable(indice == total - 1);
        try {
            pagina.setImage(cache.computeIfAbsent(indice, this::renderizar));
        } catch (RuntimeException ex) {
            alerta(Alert.AlertType.ERROR, "Vista previa", "No se pudo mostrar la página: " + ex.getMessage());
        }
    }

    private void cambiarZoom(int delta) {
        zoom = Math.max(0, Math.min(ZOOMS.length - 1, zoom + delta));
        actualizarZoom();
    }

    private void actualizarZoom() {
        double z = ZOOMS[zoom];
        pagina.setFitWidth(reporte.getPageWidth() * 1.33 * z); // puntos -> píxeles aprox.
        pagina.setPreserveRatio(true);
        pagina.setSmooth(true);
        lblZoom.setText(Math.round(z * 100) + " %");
    }

    /** Renderiza una página con JasperReports y la convierte a imagen JavaFX. */
    private Image renderizar(int i) {
        try {
            java.awt.Image awt = JasperPrintManager.printPageToImage(reporte, i, ESCALA_RENDER);
            int w = awt.getWidth(null), h = awt.getHeight(null);
            BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = bi.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(awt, 0, 0, null);
            g.dispose();
            int[] pixeles = bi.getRGB(0, 0, w, h, null, 0, w);
            WritableImage img = new WritableImage(w, h);
            img.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pixeles, 0, w);
            return img;
        } catch (Exception ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    // ----------------------------------------------------------------- descarga

    private void descargar() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar reporte PDF");
        chooser.setInitialFileName(nombreArchivo);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));
        File destino = chooser.showSaveDialog(btnDescargar.getScene().getWindow());
        if (destino == null) return;

        btnDescargar.setDisable(true);
        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ReporteService.exportarPdf(reporte, destino.toPath());
                return null;
            }
        };
        tarea.setOnSucceeded(e -> {
            btnDescargar.setDisable(false);
            Alert ok = new Alert(Alert.AlertType.INFORMATION,
                    "PDF guardado en:\n" + destino.getAbsolutePath() + "\n\n¿Desea abrirlo ahora?",
                    ButtonType.YES, ButtonType.NO);
            ok.setTitle("Reporte descargado");
            ok.setHeaderText(null);
            if (ok.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) abrirArchivo(destino);
        });
        tarea.setOnFailed(e -> {
            btnDescargar.setDisable(false);
            Throwable ex = tarea.getException();
            alerta(Alert.AlertType.ERROR, "No se pudo guardar el PDF", ex == null ? "Error desconocido" : ex.getMessage());
        });
        Thread hilo = new Thread(tarea, "exportar-pdf");
        hilo.setDaemon(true);
        hilo.start();
    }

    private static void abrirArchivo(File archivo) {
        // Desktop es de AWT: se usa en otro hilo para no bloquear JavaFX
        Thread t = new Thread(() -> {
            try {
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(archivo);
            } catch (Exception ignored) {
            }
        }, "abrir-pdf");
        t.setDaemon(true);
        t.start();
    }

    private static void alerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert a = new Alert(tipo, mensaje, ButtonType.OK);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
