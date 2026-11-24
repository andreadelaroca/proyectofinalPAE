
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

    /** Carga la vista en la ventana principal y devuelve su controlador. */
    public static <T> T abrirVentana(
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

            // Se reutiliza la escena de la ventana y solo se cambia su contenido:
            // si se creara una Scene nueva sobre una ventana maximizada, conservaría
            // su tamaño inicial y no ocuparía toda la pantalla.
            Scene escena = stagePrincipal.getScene();
            if (escena == null) {
                double ancho = Math.max(900, root.prefWidth(-1));
                double alto = Math.max(600, root.prefHeight(-1));
                escena = new Scene(root, ancho, alto);
            } else {
                escena.setRoot(root);
                escena.getStylesheets().clear();
            }
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
            pantallaCompleta(stagePrincipal);
            stagePrincipal.show();
            return loader.getController();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo cargar: " + rutaFxml,
                    e
            );
        }
    }

    /**
     * Abre la ventana ocupando toda la pantalla (maximizada: conserva la barra de
     * título y la barra de tareas). Llamar antes de show()/showAndWait().
     */
    public static void pantallaCompleta(Stage stage) {
        stage.setMaximized(true);
    }

    public static void volverLogin() {
        abrirVentana(
                "/ni/edu/uam/proyectofinalpae/fxml/login.fxml",
                "BunBun Hub - Ingreso"
        );
    }
}
