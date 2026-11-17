package ni.edu.uam.proyectofinalpae.controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalpae.model.Producto;
import ni.edu.uam.proyectofinalpae.service.ProductoService;

import java.io.IOException;
import java.math.BigDecimal;

public class ProductoController {

    @FXML private Button btnNuevoProducto;
    @FXML private TableView<Producto> tblProductos;
    @FXML private TableColumn<Producto, String> colCodigo;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colMarca;
    @FXML private TableColumn<Producto, BigDecimal> colPrecio;
    @FXML private TableColumn<Producto, Integer> colCantidad;
    @FXML private TableColumn<Producto, String> colColor;
    @FXML private TableColumn<Producto, String> colDescripcion;

    private final ProductoService productoService = new ProductoService();
    private final ObservableList<Producto> listaProductos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarColumnas();
        cargarProductos();
        btnNuevoProducto.setOnAction(e -> abrirNuevoProducto());
    }

    private void configurarColumnas() {
        colCodigo.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getCodigo()));
        colNombre.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getNombre()));
        colMarca.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getMarca()));
        colPrecio.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getPrecioUnitario()));
        colCantidad.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getCantidad()));
        colDescripcion.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getDescripcion()));

        colColor.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getColor()));

        // Muestra una pequeña muestra visual del color junto a su código hexadecimal
        colColor.setCellFactory(column -> new TableCell<>() {
            private final Rectangle muestra = new Rectangle(14, 14);

            {
                muestra.setArcWidth(4);
                muestra.setArcHeight(4);
                muestra.setStroke(Color.GRAY);
                muestra.setStrokeWidth(0.5);
            }

            @Override
            protected void updateItem(String hex, boolean empty) {
                super.updateItem(hex, empty);
                if (empty || hex == null || hex.isBlank()) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText("#" + hex.toUpperCase());
                    try {
                        muestra.setFill(Color.web("#" + hex));
                        setGraphic(muestra);
                    } catch (IllegalArgumentException e) {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private void cargarProductos() {
        listaProductos.setAll(productoService.obtenerTodos());
        tblProductos.setItems(listaProductos);
    }

    private void abrirNuevoProducto() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ni/edu/uam/proyectofinalpae/fxml/nuevo-producto-view.fxml")
            );
            Parent root = loader.load();

            NuevoProductoController controller = loader.getController();

            Stage modal = new Stage();
            modal.setTitle("BunBun Hub - Nuevo Producto");
            modal.setScene(new Scene(root));
            modal.initOwner(btnNuevoProducto.getScene().getWindow());
            modal.initModality(Modality.WINDOW_MODAL);
            modal.setResizable(false);
            modal.showAndWait();

            // Si se registró un producto, se agrega directo en memoria sin recargar toda la BD
            Producto creado = controller.getProductoGuardado();
            if (creado != null) {
                listaProductos.add(creado);
                tblProductos.getSelectionModel().select(creado);
            }
        } catch (IOException e) {
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Error");
            alerta.setHeaderText(null);
            alerta.setContentText("No se pudo abrir el formulario de nuevo producto.");
            alerta.showAndWait();
            e.printStackTrace();
        }
    }
}