package ni.edu.uam.proyectofinalpae.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import ni.edu.uam.proyectofinalpae.model.Usuario;
import ni.edu.uam.proyectofinalpae.util.Sesion;
import ni.edu.uam.proyectofinalpae.util.Ui;
import ni.edu.uam.proyectofinalpae.util.Vistas;

import java.io.IOException;

public class MainAdminController {
    @FXML private StackPane pnlContenido;
    @FXML private Button btnPedidos, btnClientes, btnColaboradores, btnProductos, btnVentas, btnInformes, btnCerrarSesion;
    @FXML private Label lblUsuario;

    @FXML
    public void initialize() {
        Usuario u = Sesion.getUsuario();
        lblUsuario.setText(u == null ? "Sin sesión" : u.getNombres() + " " + u.getApellidos());

        btnPedidos.setOnAction(e -> mostrar(Vistas.PEDIDOS));
        btnClientes.setOnAction(e -> mostrar(Vistas.CLIENTES));
        btnColaboradores.setOnAction(e -> mostrar(Vistas.COLABORADORES));
        btnProductos.setOnAction(e -> mostrar(Vistas.PRODUCTOS));
        btnVentas.setOnAction(e -> mostrar(Vistas.VENTAS));
        btnInformes.setOnAction(e -> mostrar(Vistas.INFORMES));
        btnCerrarSesion.setOnAction(e -> cerrarSesion());

        mostrar(Vistas.PEDIDOS);
    }

    private void mostrar(String fxml) {
        try {
            Vistas.cargarEn(pnlContenido, fxml);
        } catch (IOException e) {
            e.printStackTrace();
            Ui.error("Error", "No se pudo abrir la vista " + fxml + ".");
        }
    }

    private void cerrarSesion() {
        if (!Ui.confirmar("Cerrar sesión", "¿Desea cerrar la sesión?")) return;
        Sesion.cerrar();
        try {
            Vistas.cambiarEscena(btnCerrarSesion.getScene().getWindow(), Vistas.LOGIN, "BunBun Hub - Iniciar sesión");
        } catch (IOException e) {
            e.printStackTrace();
            Ui.error("Error", "No se pudo volver al inicio de sesión.");
        }
    }
}