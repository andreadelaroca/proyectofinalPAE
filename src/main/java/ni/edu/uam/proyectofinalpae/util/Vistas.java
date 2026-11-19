package ni.edu.uam.proyectofinalpae.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;

public final class Vistas {
    public static final String LOGIN = "login-view.fxml";
    public static final String REGISTRO = "registro-view.fxml";
    public static final String VERIFICACION = "verificacion-view.fxml";
    public static final String MAIN_ADMIN = "main-admin.fxml";
    public static final String CLIENTES = "clientes-view.fxml";
    public static final String COLABORADORES = "colaboradores-view.fxml";
    public static final String PEDIDOS = "pedidos-view.fxml";
    public static final String PRODUCTOS = "productos-view.fxml";
    public static final String VENTAS = "ventas-view.fxml";
    public static final String INFORMES = "informes-view.fxml";
    public static final String NUEVO_CLIENTE = "nuevo-cliente-view.fxml";
    public static final String NUEVO_COLABORADOR = "nuevo-colaborador-view.fxml";
    public static final String NUEVO_PEDIDO = "nuevo-pedido-view.fxml";
    public static final String NUEVO_PRODUCTO = "nuevo-producto-view.fxml";
    public static final String NUEVA_VENTA = "nueva-venta-view.fxml";
    public static final String MAPA_DIRECCION = "mapa-direccion-view.fxml";

    private static final String RUTA = "/ni/edu/uam/proyectofinalpae/fxml/";

    private Vistas() {}

    public record Vista<C>(Parent root, C controller) {}

    public static <C> Vista<C> cargar(String fxml) throws IOException {
        URL url = Vistas.class.getResource(RUTA + fxml);
        if (url == null) throw new IOException("No se encontró la vista " + fxml);
        FXMLLoader loader = new FXMLLoader(url);
        Parent root = loader.load();
        return new Vista<>(root, loader.getController());
    }

    public static void mostrarModal(Vista<?> vista, String titulo, Window owner) {
        Stage modal = new Stage();
        modal.setTitle(titulo);
        modal.setScene(new Scene(vista.root()));
        modal.initOwner(owner);
        modal.initModality(Modality.WINDOW_MODAL);
        modal.setResizable(false);
        modal.showAndWait();
    }

    public static void cambiarEscena(Window ventana, String fxml, String titulo) throws IOException {
        Vista<Object> vista = cargar(fxml);
        Stage stage = (Stage) ventana;
        stage.setScene(new Scene(vista.root()));
        stage.setTitle(titulo);
        stage.centerOnScreen();
    }

    public static <C> Vista<C> cargarEn(Pane contenedor, String fxml) throws IOException {
        Vista<C> vista = cargar(fxml);
        contenedor.getChildren().setAll(vista.root());
        return vista;
    }
}