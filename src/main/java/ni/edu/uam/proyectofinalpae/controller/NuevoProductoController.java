package ni.edu.uam.proyectofinalpae.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalpae.model.Producto;
import ni.edu.uam.proyectofinalpae.service.ProductoService;

import java.io.File;
import java.math.BigDecimal;

public class NuevoProductoController {

    @FXML private Label lblTitulo;
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
    private Producto productoEdicion;
    private boolean guardadoExitoso = false;

    @FXML
    public void initialize() {
        lblMensaje.setText("");
        txtMarca.setText("BunBun");
        cpColor.setValue(Color.web("#C8D9E6"));
        txtCodigo.setText("Calculando...");

        Task<String> tareaCodigo = new Task<>() {
            @Override
            protected String call() {
                return productoService.obtenerSiguienteCodigo();
            }
        };
        tareaCodigo.setOnSucceeded(e -> {
            // Solo asigna el código automático si no se cargó un producto para editar
            if (productoEdicion == null) {
                txtCodigo.setText(tareaCodigo.getValue());
            }
        });
        tareaCodigo.setOnFailed(e -> {
            if (productoEdicion == null) {
                txtCodigo.setText("BUN-AUTO");
            }
        });
        Thread hilo = new Thread(tareaCodigo);
        hilo.setDaemon(true);
        hilo.start();

        btnExaminar.setOnAction(e -> seleccionarImagen());
        btnCancelar.setOnAction(e -> cerrarVentana());
        btnGuardar.setOnAction(e -> guardarProducto());
    }

    public void cargarProductoParaEditar(Producto producto) {
        this.productoEdicion = producto;
        if (lblTitulo != null) {
            lblTitulo.setText("Editar Producto");
        }
        btnGuardar.setText("Guardar Cambios");

        txtCodigo.setText(producto.getCodigo());
        txtMarca.setText(producto.getMarca());
        txtNombre.setText(producto.getNombre());
        txtDescripcion.setText(producto.getDescripcion());
        txtPrecio.setText(producto.getPrecioUnitario() != null ? producto.getPrecioUnitario().toPlainString() : "");
        txtCantidad.setText(producto.getCantidad() != null ? String.valueOf(producto.getCantidad()) : "0");
        txtRutaImagen.setText(producto.getRutaImagen());

        if (producto.getColor() != null && !producto.getColor().isBlank()) {
            try {
                String hex = producto.getColor().startsWith("#") ? producto.getColor() : "#" + producto.getColor();
                cpColor.setValue(Color.web(hex));
            } catch (IllegalArgumentException ignored) {
                cpColor.setValue(Color.web("#C8D9E6"));
            }
        }
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

        boolean esEdicion = (productoEdicion != null);

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

        btnGuardar.setDisable(true);
        String hexColor = colorAHex(cpColor.getValue());

        Task<Producto> tareaGuardar = new Task<>() {
            @Override
            protected Producto call() {
                Producto target = esEdicion ? productoEdicion : new Producto();
                target.setNombre(nombre);
                target.setMarca(marca);
                target.setDescripcion(descripcion);
                target.setPrecioUnitario(precio);
                target.setCantidad(cantidad);
                target.setColor(hexColor);
                target.setRutaImagen(rutaImagen);

                if (esEdicion) {
                    productoService.actualizarProducto(target);
                    return target;
                } else {
                    return productoService.registrarProducto(target);
                }
            }
        };

        tareaGuardar.setOnSucceeded(e -> {
            btnGuardar.setDisable(false);
            guardadoExitoso = true;

            if (!esEdicion) {
                productoGuardado = tareaGuardar.getValue();
            }

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle(esEdicion ? "Producto Actualizado" : "Producto Registrado");
            info.setHeaderText(null);
            info.setContentText(esEdicion
                    ? "El producto " + tareaGuardar.getValue().getCodigo() + " fue actualizado correctamente."
                    : "Producto guardado exitosamente con el código: " + productoGuardado.getCodigo());
            info.showAndWait();

            cerrarVentana();
        });

        tareaGuardar.setOnFailed(e -> {
            btnGuardar.setDisable(false);
            lblMensaje.setText("Error al guardar el producto en la base de datos.");
            tareaGuardar.getException().printStackTrace();
        });

        Thread hilo = new Thread(tareaGuardar);
        hilo.setDaemon(true);
        hilo.start();
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

    public boolean isGuardadoExitoso() {
        return guardadoExitoso;
    }
}