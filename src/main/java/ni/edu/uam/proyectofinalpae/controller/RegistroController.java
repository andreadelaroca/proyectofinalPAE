
package ni.edu.uam.proyectofinalpae.controller;

import javafx.fxml.FXML;
import ni.edu.uam.proyectofinalpae.util.SceneManager;

public class RegistroController {

    @FXML
    public void registroCliente() {
        SceneManager.abrirVentana(
                "/ni/edu/uam/proyectofinalpae/fxml/nuevo-cliente-view.fxml",
                "BunBun Hub - Registro de cliente"
        );
    }

    @FXML
    public void volverLogin() {
        SceneManager.volverLogin();
    }
}
