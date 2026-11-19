package ni.edu.uam.proyectofinalpae.application;

import javafx.application.Application;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalpae.config.HibernateUtil;
import ni.edu.uam.proyectofinalpae.util.SceneManager;

public class MainApplication extends Application {
    @Override public void start(Stage stage) {
        SceneManager.setStage(stage);
        stage.setMinWidth(760);
        stage.setMinHeight(550);
        SceneManager.abrirVentana("/ni/edu/uam/proyectofinalpae/fxml/login.fxml", "BunBun Hub - Ingreso");
    }
    @Override public void stop() {
        try { HibernateUtil.cerrar(); } catch (Exception ignored) {}
    }
}