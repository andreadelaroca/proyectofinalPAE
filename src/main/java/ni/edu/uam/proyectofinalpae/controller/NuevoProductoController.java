package ni.edu.uam.proyectofinalpae.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import ni.edu.uam.proyectofinalpae.model.Marca;
import ni.edu.uam.proyectofinalpae.model.Producto;
import ni.edu.uam.proyectofinalpae.service.MarcaService;
import ni.edu.uam.proyectofinalpae.service.ProductoService;
import ni.edu.uam.proyectofinalpae.util.Async;
import ni.edu.uam.proyectofinalpae.util.NegocioException;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class NuevoProductoController {

    @FXML private TextField txtCodigo;
    @FXML private ComboBox<Marca> cbMarca;   // CAMBIO: antes era TextField txtMarca. Editable: escribir una marca nueva la crea.
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
    private final MarcaService marcaService = new MarcaService();
    private Producto productoGuardado;

    @FXML
    public void initialize() {
        lblMensaje.setText("");
        cpColor.setValue(Color.web("#C8D9E6"));
        txtCodigo.setEditable(false);
        txtCodigo.setText("BUN-AUTO");

        cbMarca.setEditable(true);
        cbMarca.setConverter(new StringConverter<>() {
            @Override
            public String toString(Marca m) {
                return m == null ? "" : m.getNombre();
            }

            @Override
            public Marca fromString(String texto) {
                return cbMarca.getItems().stream()
                        .filter(m -> m.getNombre().equalsIgnoreCase(texto == null ? "" : texto.trim()))
                        .findFirst().orElse(null);
            }
        });

        // Carga marcas y código sugerido fuera del hilo de JavaFX
        Async.ejecutar(marcaService::obtenerTodas, marcas -> {
            cbMarca.setItems(FXCollections.observableArrayList(marcas));
            marcas.stream().filter(m -> m.getNombre().equalsIgnoreCase("BunBun")).findFirst()
                    .ifPresentOrElse(cbMarca::setValue, () -> cbMarca.getEditor().setText("BunBun"));
        }, error -> cbMarca.getEditor().setText("BunBun"));
        Async.ejecutar(productoService::obtenerSiguienteCodigo, txtCodigo::setText, error -> { });

        btnExaminar.setOnAction(e -> seleccionarImagen());
        btnCancelar.setOnAction(e -> cerrarVentana());
        btnGuardar.setOnAction(e -> guardarProducto());
    }

    private void seleccionarImagen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar imagen del producto");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.webp"));

        File archivo = fileChooser.showOpenDialog(btnExaminar.getScene().getWindow());
        if (archivo != null) txtRutaImagen.setText(archivo.getAbsolutePath());
    }

    private void guardarProducto() {
        lblMensaje.setText("");

        String nombre = txtNombre.getText().trim();
        String marcaTexto = cbMarca.getEditor().getText().trim();
        String descripcion = txtDescripcion.getText().trim();
        String precioTxt = txtPrecio.getText().trim();
        String cantidadTxt = txtCantidad.getText().trim();
        String rutaOrigen = txtRutaImagen.getText().trim();

        if (nombre.isEmpty() || marcaTexto.isEmpty() || descripcion.isEmpty()
                || precioTxt.isEmpty() || cantidadTxt.isEmpty() || rutaOrigen.isEmpty()) {
            lblMensaje.setText("Todos los campos son obligatorios.");
            return;
        }
        if (nombre.length() > 70) { lblMensaje.setText("El nombre no puede superar los 70 caracteres."); return; }
        if (marcaTexto.length() > 40) { lblMensaje.setText("La marca no puede superar los 40 caracteres."); return; }
        if (descripcion.length() > 120) { lblMensaje.setText("La descripción no puede superar los 120 caracteres."); return; }

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

        String colorHex = colorAHex(cpColor.getValue());
        btnGuardar.setDisable(true);

        Async.ejecutar(() -> {
            Producto nuevo = new Producto();
            nuevo.setNombre(nombre);
            nuevo.setMarca(marcaService.obtenerOCrear(marcaTexto));
            nuevo.setDescripcion(descripcion);
            nuevo.setPrecioUnitario(precio);
            nuevo.setCantidad(cantidad);
            nuevo.setColor(colorHex);
            nuevo.setRutaImagen(copiarImagen(Path.of(rutaOrigen)));
            return productoService.registrarProducto(nuevo);
        }, guardado -> {
            btnGuardar.setDisable(false);
            productoGuardado = guardado;
            ni.edu.uam.proyectofinalpae.util.Ui.info("Producto Registrado",
                    "Producto guardado exitosamente con el código: " + guardado.getCodigo());
            cerrarVentana();
        }, e -> {
            btnGuardar.setDisable(false);
            lblMensaje.setText(Async.mensaje(e));
        });
    }

    /**
     * Copia la imagen a ~/BunBunHub/images y devuelve esa ruta absoluta.
     * (Antes se guardaba "/images/nombre.png" sin copiar el archivo a ningún lado.)
     */
    private String copiarImagen(Path origen) {
        try {
            Path carpeta = Path.of(System.getProperty("user.home"), "BunBunHub", "images");
            Files.createDirectories(carpeta);
            String nombre = origen.getFileName().toString();
            String extension = nombre.contains(".") ? nombre.substring(nombre.lastIndexOf('.')) : "";
            Path destino = carpeta.resolve(UUID.randomUUID() + extension);
            Files.copy(origen, destino, StandardCopyOption.REPLACE_EXISTING);
            return destino.toString();
        } catch (IOException e) {
            throw new NegocioException("No se pudo copiar la imagen seleccionada.");
        }
    }

    private String colorAHex(Color color) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("%02X%02X%02X", r, g, b);
    }

    private void cerrarVentana() {
        ((Stage) btnCancelar.getScene().getWindow()).close();
    }

    public Producto getProductoGuardado() {
        return productoGuardado;
    }
}