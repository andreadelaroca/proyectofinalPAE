package ni.edu.uam.proyectofinalpae.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Path;

public final class Ui {
    private Ui() {}

    public static void info(String titulo, String mensaje) { mostrar(Alert.AlertType.INFORMATION, titulo, mensaje); }
    public static void advertencia(String titulo, String mensaje) { mostrar(Alert.AlertType.WARNING, titulo, mensaje); }
    public static void error(String titulo, String mensaje) { mostrar(Alert.AlertType.ERROR, titulo, mensaje); }

    public static boolean confirmar(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.YES, ButtonType.NO);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        return alerta.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }

    private static void mostrar(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    public static String ocultarCorreo(String correo) {
        if (correo == null) return "";
        int arroba = correo.indexOf('@');
        return arroba < 1 ? correo : correo.charAt(0) + "***" + correo.substring(arroba);
    }

    public static Path elegirDestinoPdf(Window owner, String nombreSugerido) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar PDF");
        fc.setInitialFileName(nombreSugerido);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File archivo = fc.showSaveDialog(owner);
        return archivo == null ? null : archivo.toPath();
    }

    public static void abrir(Path archivo) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(archivo.toFile());
        } catch (Exception ignorada) {
        }
    }

    public static void campoCodigoProducto(TextField campo) {
        campo.setText("BUN-");
        campo.setTextFormatter(new TextFormatter<>(change -> {
            String nuevoTexto = change.getControlNewText();

            if (nuevoTexto.matches("\\d+")) {
                String completo = "BUN-" + nuevoTexto;
                change.setRange(0, change.getControlText().length());
                change.setText(completo);
                change.setCaretPosition(completo.length());
                change.setAnchor(completo.length());
                return change;
            }
            if (nuevoTexto.length() < 4 || !nuevoTexto.substring(0, 4).equalsIgnoreCase("BUN-")) {
                return null;
            }
            if (nuevoTexto.toUpperCase().startsWith("BUN-BUN-")) {
                String limpio = "BUN-" + nuevoTexto.substring(8).toUpperCase();
                change.setRange(0, change.getControlText().length());
                change.setText(limpio);
                change.setCaretPosition(limpio.length());
                change.setAnchor(limpio.length());
                return change;
            }
            change.setText(change.getText().toUpperCase());
            return change;
        }));
    }
}