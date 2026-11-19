
package ni.edu.uam.proyectofinalpae.application;

import javafx.application.Application;
import javafx.stage.Stage;
<<<<<<< HEAD
import ni.edu.uam.proyectofinalpae.config.HibernateUtil;
import ni.edu.uam.proyectofinalpae.util.SceneManager;
=======

import java.io.IOException;
>>>>>>> origin/main

public class MainApplication extends Application {

    @Override
<<<<<<< HEAD
    public void start(Stage stage) {
        SceneManager.setStage(stage);

        stage.setMinWidth(760);
        stage.setMinHeight(550);

        SceneManager.abrirVentana(
                "/ni/edu/uam/proyectofinalpae/fxml/login.fxml",
                "BunBun Hub - Ingreso"
        );
    }

    @Override
    public void stop() {
        try {
            HibernateUtil.cerrar();
        } catch (Exception ignored) {
        }
    }

    public static void main(String[] args) {
        launch(args);
=======
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("/ni/edu/uam/proyectofinalpae/fxml/login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Ventana de Administrador");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
>>>>>>> origin/main
    }
}
