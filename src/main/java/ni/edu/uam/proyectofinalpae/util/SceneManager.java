
package ni.edu.uam.proyectofinalpae.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public final class SceneManager {

    private static Stage stagePrincipal;

    private static final String RUTA_CSS =
            "/ni/edu/uam/proyectofinalpae/styles/";

    private SceneManager() {}

    public static void setStage(Stage stage) {
        stagePrincipal = stage;
    }

    public static void aplicarEstilos(Scene escena) {
        URL global = Objects.requireNonNull(
                SceneManager.class.getResource(
                        RUTA_CSS + "global.css"),
                "No se encontró global.css"
        );

        String ruta = global.toExternalForm();

        if (!escena.getStylesheets().contains(ruta)) {
            escena.getStylesheets().add(ruta);
        }
    }

    public static void abrirVentana(
            String rutaFxml,
            String titulo
    ) {
        if (stagePrincipal == null) {
            throw new IllegalStateException(
                    "Stage principal no configurado"
            );
        }

        try {
            URL recurso = Objects.requireNonNull(
                    SceneManager.class.getResource(rutaFxml),
                    "FXML no encontrado: " + rutaFxml
            );

            FXMLLoader loader = new FXMLLoader(recurso);
            Parent root = loader.load();

            Scene escena = new Scene(root, 900, 600);
            aplicarEstilos(escena);

            if (rutaFxml.endsWith("/login.fxml")
                    || rutaFxml.endsWith("/login.fxml")) {

                URL loginCss = Objects.requireNonNull(
                        SceneManager.class.getResource(
                                RUTA_CSS + "login.css"),
                        "No se encontró login.css"
                );

                escena.getStylesheets().add(
                        loginCss.toExternalForm()
                );
            }

            stagePrincipal.setTitle(titulo);
            stagePrincipal.setScene(escena);
            stagePrincipal.show();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo cargar: " + rutaFxml,
                    e
            );
        }
    }

    public static void volverLogin() {
        abrirVentana(
                "/ni/edu/uam/proyectofinalpae/fxml/login.fxml",
                "BunBun Hub - Ingreso"
        );
    }
}
