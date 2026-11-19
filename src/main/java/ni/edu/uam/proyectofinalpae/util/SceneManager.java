package ni.edu.uam.proyectofinalpae.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalpae.application.MainApplication;

import java.io.IOException;
import java.net.URL;

public final class SceneManager {
    private static final double ANCHO = 900;
    private static final double ALTO = 600;

    private static Stage stagePrincipal;

    private SceneManager() {
    }

    public static void setStage(Stage stage) {
        stagePrincipal = stage;
    }

    public static void abrirVentana(String rutaFxml, String titulo) {
        try {
            if (stagePrincipal == null) {
                throw new IllegalStateException("SceneManager no tiene Stage");
            }
            URL url = SceneManager.class.getResource(rutaFxml);
            Parent root = FXMLLoader.load(url);
            stagePrincipal.setTitle(titulo);
            stagePrincipal.setScene(new Scene(root, ANCHO, ALTO));
            stagePrincipal.show();
        } catch (IOException e) {
            System.out.println("No se encontró el archivo FXML:" + rutaFxml);
        }
    }
}