package ni.edu.uam.proyectofinalpae.controller;

import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.animation.Transition;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.io.IOException;

public class MainAdminController {

    private static final Color COLOR_TARJETA_NORMAL = Color.web("#FFFFFF");
    private static final Color COLOR_TARJETA_HOVER  = Color.web("#EAE3DB");

    @FXML
    void abrirPedidos(MouseEvent event) {
        abrirVentanaMaximizada("pedidos-view.fxml", "Gestión de Pedidos");
    }

    @FXML
    void abrirClientes(MouseEvent event) {
        abrirVentanaMaximizada("clientes-view.fxml", "Registro de Clientes");
    }

    @FXML
    void abrirInformes(MouseEvent event) {
        abrirVentanaMaximizada("informes-view.fxml", "Generación de Informes");
    }

    private void abrirVentanaMaximizada(String archivoFxml, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ni/edu/uam/proyectofinalpae/fxml/" + archivoFxml)
            );
            Parent root = loader.load();

            Stage nuevaVentana = new Stage();
            nuevaVentana.setTitle(titulo);
            nuevaVentana.setScene(new Scene(root));

            // 1. Obtener la ventana activa (main_admin) y asignarla como padre
            Window ventanaPadre = Window.getWindows().stream()
                    .filter(Window::isFocused)
                    .findFirst()
                    .orElse(null);
            if (ventanaPadre != null) {
                nuevaVentana.initOwner(ventanaPadre);
            }

            // 2. Bloquear interacción con main_admin mientras esta vista esté abierta
            nuevaVentana.initModality(Modality.WINDOW_MODAL);
            nuevaVentana.setMaximized(true);

            // 3. Esperar a que el usuario cierre la ventana antes de devolver el control
            nuevaVentana.showAndWait();

            // Opcional: si modificaste datos en la ventana hija, aquí puedes refrescar main_admin
            // cargarResumenDashboard();

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error de navegación",
                    "No se pudo abrir la vista: " + archivoFxml + "\n" + e.getMessage());
        }
    }

    @FXML
    void cerrarSesion(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ni/edu/uam/proyectofinalpae/fxml/login-view.fxml")
            );
            Parent root = loader.load();

            Stage loginStage = new Stage();
            loginStage.setTitle("Inicio de Sesión");
            loginStage.setScene(new Scene(root));
            loginStage.show();

            Stage ventanaActual = (Stage) ((Node) event.getSource()).getScene().getWindow();
            ventanaActual.close();

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo regresar al inicio de sesión.");
        }
    }

    @FXML
    void agrandarIcono(MouseEvent event) {
        animarEscala((Node) event.getSource(), 1.15);
    }

    @FXML
    void restaurarIcono(MouseEvent event) {
        animarEscala((Node) event.getSource(), 1.0);
    }

    @FXML
    void oscurecerTarjeta(MouseEvent event) {
        animarFondoTarjeta((Region) event.getSource(), COLOR_TARJETA_NORMAL, COLOR_TARJETA_HOVER);
    }

    @FXML
    void aclararTarjeta(MouseEvent event) {
        animarFondoTarjeta((Region) event.getSource(), COLOR_TARJETA_HOVER, COLOR_TARJETA_NORMAL);
    }

    @FXML
    private void abrirProductos() {
        abrirVentanaMaximizada("productos-view.fxml", "BunBun Hub - Gestión de Productos");
    }

    @FXML
    private void abrirColaboradores() {
        // Lógica pendiente para el módulo de colaboradores
        mostrarAlerta(Alert.AlertType.INFORMATION, "Módulo en desarrollo",
                "La gestión de colaboradores estará disponible próximamente.");
    }

    private void animarEscala(Node nodo, double escalaDestino) {
        ScaleTransition st = new ScaleTransition(Duration.millis(150), nodo);
        st.setToX(escalaDestino);
        st.setToY(escalaDestino);
        st.play();
    }

    public void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void animarFondoTarjeta(Region tarjeta, Color inicio, Color fin) {
        if (tarjeta.getProperties().get("animFondo") instanceof Transition anterior) {
            anterior.stop();
        }

        Transition transicion = new Transition() {
            {
                setCycleDuration(Duration.millis(220));
            }

            @Override
            protected void interpolate(double frac) {
                Color actual = inicio.interpolate(fin, frac);
                String hex = String.format("#%02X%02X%02X",
                        (int) (actual.getRed() * 255),
                        (int) (actual.getGreen() * 255),
                        (int) (actual.getBlue() * 255));
                tarjeta.setStyle("-fx-background-color: " + hex + ";");
            }
        };

        tarjeta.getProperties().put("animFondo", transicion);
        transicion.play();
    }
}