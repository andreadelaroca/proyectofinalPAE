package ni.edu.uam.proyectofinalpae.controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import ni.edu.uam.proyectofinalpae.model.Producto;
import ni.edu.uam.proyectofinalpae.service.ProductoService;
import ni.edu.uam.proyectofinalpae.util.Async;
import ni.edu.uam.proyectofinalpae.util.Ui;
import ni.edu.uam.proyectofinalpae.util.Vistas;

import java.io.IOException;
import java.math.BigDecimal;

/** Antes: ProductoController (renombrado para coincidir con productos-view.fxml). */
public class ProductosController {

    @FXML private Button btnNuevoProducto;
    @FXML private TableView<Producto> tblProductos;
    @FXML private TableColumn<Producto, String> colCodigo;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colMarca;
    @FXML private TableColumn<Producto, BigDecimal> colPrecio;
    @FXML private TableColumn<Producto, Integer> colCantidad;
    @FXML private TableColumn<Producto, String> colColor;
    @FXML private TableColumn<Producto, String> colDescripcion;
    @FXML private Button btnRefrescar;
    @FXML private TextField txtBuscar;

    private final ProductoService productoService = new ProductoService();
    private final ObservableList<Producto> listaProductos = FXCollections.observableArrayList();
    private FilteredList<Producto> listaFiltrada;

    @FXML
    public void initialize() {
        configurarColumnas();

        listaFiltrada = new FilteredList<>(listaProductos, p -> true);
        tblProductos.setItems(listaFiltrada);
        configurarFiltro();

        btnNuevoProducto.setOnAction(e -> abrirNuevoProducto());
        btnRefrescar.setOnAction(e -> cargarProductos());

        cargarProductos();
    }

    private void configurarColumnas() {
        colCodigo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCodigo()));
        colNombre.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNombre()));
        // marca ahora es una entidad (Producto.marca -> Marca)
        colMarca.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getMarca() == null ? "" : cell.getValue().getMarca().getNombre()));
        colPrecio.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getPrecioUnitario()));
        colCantidad.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getCantidad()));
        colDescripcion.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescripcion()));
        colColor.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getColor()));

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
        tblProductos.setPlaceholder(new Label("Cargando productos..."));
        btnRefrescar.setDisable(true);

        // CORREGIDO: ya no se hace tblProductos.setItems(listaProductos), que anulaba el filtro de búsqueda.
        Async.ejecutar(productoService::obtenerTodos, lista -> {
            listaProductos.setAll(lista);
            tblProductos.setPlaceholder(new Label("No hay productos registrados."));
            btnRefrescar.setDisable(false);
        }, error -> {
            tblProductos.setPlaceholder(new Label("Error al cargar los productos."));
            btnRefrescar.setDisable(false);
            Async.mensaje(error);
        });
    }

    private void abrirNuevoProducto() {
        try {
            Vistas.Vista<NuevoProductoController> vista = Vistas.cargar(Vistas.NUEVO_PRODUCTO);
            Vistas.mostrarModal(vista, "BunBun Hub - Nuevo Producto", btnNuevoProducto.getScene().getWindow());

            Producto creado = vista.controller().getProductoGuardado();
            if (creado != null) {
                listaProductos.add(creado);
                tblProductos.getSelectionModel().select(creado);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Ui.error("Error", "No se pudo abrir el formulario de nuevo producto.");
        }
    }

    private void configurarFiltro() {
        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> {
            String filtro = (nuevo == null) ? "" : nuevo.toLowerCase().trim();
            listaFiltrada.setPredicate(prod -> {
                if (filtro.isEmpty()) return true;
                String marca = prod.getMarca() == null ? "" : prod.getMarca().getNombre().toLowerCase();
                return (prod.getCodigo() != null && prod.getCodigo().toLowerCase().contains(filtro))
                        || (prod.getNombre() != null && prod.getNombre().toLowerCase().contains(filtro))
                        || marca.contains(filtro);
            });
        });
    }
}