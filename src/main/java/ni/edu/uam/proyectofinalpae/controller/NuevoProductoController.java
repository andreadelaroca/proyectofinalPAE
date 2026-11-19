package ni.edu.uam.proyectofinalpae.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalpae.model.Producto;
import ni.edu.uam.proyectofinalpae.service.ProductoService;
import ni.edu.uam.proyectofinalpae.service.MarcaService;

import java.io.File;
import java.math.BigDecimal;

public class NuevoProductoController {

    @FXML private TextField txtCodigo;
    @FXML private TextField txtMarca;
    @FXML private TextField txtNombre;
    @FXML private TextArea txtDescripcion;
    @FXML private TextField txtPrecio;
    @FXML private TextField txtCantidad;
    @FXML private ColorPicker cpColor;
    @FXML private TextField txtRutaImagen;
    @FXML private Button btnExaminar;
    @FXML private Label lblMensaje;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardar;

    private final ProductoService productoService = new ProductoService();
    private Producto productoGuardado;

    @FXML
    public void initialize() {
        lblMensaje.setText("");
        txtMarca.setText("BunBun");
        cpColor.setValue(Color.web("#C8D9E6"));

        try {
            txtCodigo.setText(productoService.obtenerSiguienteCodigo());
        } catch (Exception e) {
            txtCodigo.setText("BUN-AUTO");
        }

        btnExaminar.setOnAction(e -> seleccionarImagen());
        btnCancelar.setOnAction(e -> cerrarVentana());
        btnGuardar.setOnAction(e -> guardarProducto());
    }

    private void seleccionarImagen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar imagen del producto");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        File archivo = fileChooser.showOpenDialog(btnExaminar.getScene().getWindow());
        if (archivo != null) {
            txtRutaImagen.setText("/images/" + archivo.getName());
        }
    }

    private void guardarProducto() {
        lblMensaje.setText("");

        String nombre = txtNombre.getText().trim();
        String marca = txtMarca.getText().trim();
        String descripcion = txtDescripcion.getText().trim();
        String precioTxt = txtPrecio.getText().trim();
        String cantidadTxt = txtCantidad.getText().trim();
        String rutaImagen = txtRutaImagen.getText().trim();

        if (nombre.isEmpty() || marca.isEmpty() || descripcion.isEmpty()
                || precioTxt.isEmpty() || cantidadTxt.isEmpty() || rutaImagen.isEmpty()) {
            lblMensaje.setText("Todos los campos son obligatorios.");
            return;
        }

        if (nombre.length() > 70) {
            lblMensaje.setText("El nombre no puede superar los 70 caracteres.");
            return;
        }
        if (marca.length() > 30) {
            lblMensaje.setText("La marca no puede superar los 30 caracteres.");
            return;
        }
        if (descripcion.length() > 120) {
            lblMensaje.setText("La descripción no puede superar los 120 caracteres.");
            return;
        }

        BigDecimal precio;
        int cantidad;
        try {
            precio = new BigDecimal(precioTxt);
            if (precio.compareTo(BigDecimal.ZERO) <= 0) {
                lblMensaje.setText("El precio unitario debe ser mayor a 0.");
                return;
            }
        } catch (NumberFormatException e) {
            lblMensaje.setText("Ingrese un precio unitario válido (ej. 25.50).");
            return;
        }

        try {
            cantidad = Integer.parseInt(cantidadTxt);
            if (cantidad < 0) {
                lblMensaje.setText("La cantidad en stock no puede ser negativa.");
                return;
            }
        } catch (NumberFormatException e) {
            lblMensaje.setText("Ingrese una cantidad entera válida.");
            return;
        }

        try {
            Producto nuevo = new Producto();
            nuevo.setNombre(nombre);
            nuevo.setMarca(new ni.edu.uam.proyectofinalpae.service.MarcaService().obtenerOCrear(marca));
            nuevo.setDescripcion(descripcion);
            nuevo.setPrecioUnitario(precio);
            nuevo.setCantidad(cantidad);
            nuevo.setColor(colorAHex(cpColor.getValue()));
            nuevo.setRutaImagen(rutaImagen);

            productoGuardado = productoService.registrarProducto(nuevo);

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Producto Registrado");
            info.setHeaderText(null);
            info.setContentText("Producto guardado exitosamente con el código: " + productoGuardado.getCodigo());
            info.showAndWait();

            cerrarVentana();
        } catch (Exception e) {
            lblMensaje.setText("Error al guardar el producto en la base de datos.");
            e.printStackTrace();
        }
    }

    private String colorAHex(Color color) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("%02X%02X%02X", r, g, b);
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    public Producto getProductoGuardado() {
        return productoGuardado;
    }
}