package ni.edu.uam.proyectofinalpae.application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalpae.service.PedidoService;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("/ni/edu/uam/proyectofinalpae/fxml/login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Ventana de Administrador");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }
}
