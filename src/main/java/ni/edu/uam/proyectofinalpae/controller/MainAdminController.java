package ni.edu.uam.proyectofinalpae.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import ni.edu.uam.proyectofinalpae.util.PestanaManager;
import ni.edu.uam.proyectofinalpae.util.SceneManager;

/**
 * Panel de administración: cada módulo (pedidos, ventas, productos, clientes,
 * colaboradores e informes) se incluye como pestaña desde su propio FXML y tiene
 * su propio controlador. Los formularios "Nuevo/Editar" se abren como pestañas
 * adicionales mediante {@link PestanaManager}.
 */
public class MainAdminController {

    @FXML private TabPane tabPrincipal;

    @FXML
    public void initialize() {
        PestanaManager.registrar(tabPrincipal);
    }

    @FXML
    private void cerrarSesion() {
        PestanaManager.desregistrar();
        SceneManager.volverLogin();
    }

    @FXML
    public void abrirRegistro() {
        SceneManager.abrirVentana(
                "/ni/edu/uam/proyectofinalpae/fxml/nuevo-cliente-view.fxml",
                "BunBun Hub - Registro"
        );
    }

}
