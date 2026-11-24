package ni.edu.uam.proyectofinalpae.util;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Abre formularios como pestañas dentro del TabPane del panel de administración,
 * en lugar de crear ventanas nuevas. Si no hay un TabPane registrado (p. ej. la
 * vista se usa fuera del panel admin) se usa una ventana modal como respaldo.
 */
public final class PestanaManager {

    private static final String CLAVE_AL_CERRAR = "pestana.alCerrar";

    private static TabPane tabPane;

    private PestanaManager() {}

    public static void registrar(TabPane pane) {
        tabPane = pane;
    }

    public static void desregistrar() {
        tabPane = null;
    }

    /**
     * Muestra {@code contenido} en una pestaña cerrable y la selecciona.
     * {@code alCerrar} se ejecuta una sola vez cuando la pestaña se cierra
     * (por el usuario o desde el formulario con {@link #cerrar(Node)}).
     */
    public static void abrir(Parent contenido, String titulo, Node origen, Runnable alCerrar) {
        Runnable callback = alCerrar == null ? () -> {} : alCerrar;

        if (tabPane == null || tabPane.getScene() == null) {
            abrirModal(contenido, titulo, origen);
            callback.run();
            return;
        }

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("contenedor-pestana");

        Tab tab = new Tab(titulo, scroll);
        tab.setClosable(true);
        Tab anterior = tabPane.getSelectionModel().getSelectedItem();

        boolean[] ejecutado = {false};
        Runnable unaVez = () -> {
            if (ejecutado[0]) return;
            ejecutado[0] = true;
            if (anterior != null && tabPane.getTabs().contains(anterior)) {
                tabPane.getSelectionModel().select(anterior);
            }
            callback.run();
        };
        tab.getProperties().put(CLAVE_AL_CERRAR, unaVez);
        tab.setOnClosed(e -> unaVez.run());

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    /**
     * Cierra la pestaña (o la ventana) que contiene {@code nodo}.
     * @return true si se cerró algo.
     */
    public static boolean cerrar(Node nodo) {
        Tab tab = buscarPestana(nodo);
        if (tab != null) {
            tabPane.getTabs().remove(tab);
            Object cb = tab.getProperties().get(CLAVE_AL_CERRAR);
            if (cb instanceof Runnable r) r.run();
            return true;
        }
        if (nodo.getScene() != null && nodo.getScene().getWindow() instanceof Stage stage) {
            stage.close();
            return true;
        }
        return false;
    }

    /** true si el nodo está mostrado dentro de una pestaña del panel. */
    public static boolean estaEnPestana(Node nodo) {
        return buscarPestana(nodo) != null;
    }

    private static Tab buscarPestana(Node nodo) {
        if (tabPane == null || nodo == null) return null;
        for (Tab t : tabPane.getTabs()) {
            Node contenido = t.getContent();
            for (Node n = nodo; n != null; n = n.getParent()) {
                if (n == contenido) return t;
            }
        }
        return null;
    }

    private static void abrirModal(Parent contenido, String titulo, Node origen) {
        Stage modal = new Stage();
        modal.setTitle("BunBun Hub - " + titulo);
        Scene escena = new Scene(contenido);
        SceneManager.aplicarEstilos(escena);
        modal.setScene(escena);
        Window owner = origen != null && origen.getScene() != null ? origen.getScene().getWindow() : null;
        if (owner != null) {
            modal.initOwner(owner);
            modal.initModality(Modality.WINDOW_MODAL);
        }
        SceneManager.pantallaCompleta(modal);
        modal.showAndWait();
    }
}
