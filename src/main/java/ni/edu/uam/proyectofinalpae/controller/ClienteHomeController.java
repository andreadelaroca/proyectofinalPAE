package ni.edu.uam.proyectofinalpae.controller;
import javafx.fxml.FXML;
import ni.edu.uam.proyectofinalpae.util.SceneManager;
public class ClienteHomeController {
    @FXML public void salir() {
        SceneManager.abrirVentana("/ni/edu/uam/proyectofinalpae/fxml/login.fxml", "BunBun Hub - Ingreso");
    }
}