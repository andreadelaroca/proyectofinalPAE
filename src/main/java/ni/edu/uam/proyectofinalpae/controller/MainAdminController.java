package ni.edu.uam.proyectofinalpae.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.proyectofinalpae.model.Cliente;
import ni.edu.uam.proyectofinalpae.model.Direccion;
import ni.edu.uam.proyectofinalpae.model.Pedido;
import ni.edu.uam.proyectofinalpae.service.ClienteService;
import ni.edu.uam.proyectofinalpae.service.PedidoService;
import ni.edu.uam.proyectofinalpae.report.ReporteService;
import javafx.stage.FileChooser;
import java.io.File;
import static ni.edu.uam.proyectofinalpae.util.Utils.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MainAdminController implements Initializable {
    @FXML private TableView<Pedido> tblPedidos;
    @FXML private TableColumn<Pedido, Long> colIdPedido;
    @FXML private TableColumn<Pedido, String> colFecha;
    @FXML private TableColumn<Pedido, String> colEstado; // TODO: Agregar atributo estado a la clase Pedido
    @FXML private TableColumn<Pedido, String> colTotal;
    @FXML private TableColumn<Pedido, String> colDescripcion;
    @FXML private TableColumn<Pedido, String> colCliente;
    @FXML private TableColumn<Pedido, String> colColaborador;

    @FXML private TextField txtRegUsuario;   // TODO: Agregar atributo usuario en Cliente
    @FXML private PasswordField txtRegPassword; // TODO: Agregar atributo password en Cliente
    @FXML private TextField txtPrimerNombre;
    @FXML private TextField txtApellido;
    @FXML private TextField txtEmail;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtDireccion;
    @FXML private Button btnMapa;
    private java.math.BigDecimal latitudSeleccionada;
    private java.math.BigDecimal longitudSeleccionada;

    @FXML private DatePicker dpFechaInicio;
    @FXML private DatePicker dpFechaFinal;
    @FXML private ComboBox<String> cbTipoInforme;
    @FXML private TableView<?> tblInformes; // Tipo genérico temporal, ajustarlo a un DTO de reportes
    @FXML private TableColumn<?, ?> colConcepto;
    @FXML private TableColumn<?, ?> colValor;

    private final PedidoService pedidoService = new PedidoService();
    private final ClienteService clienteService = new ClienteService();

    private ObservableList<Pedido> pedidosObservableList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarTablaPedidos();
        cargarDatosPedidos();

        if (btnMapa != null) btnMapa.setOnAction(e ->
                ni.edu.uam.proyectofinalpae.map.MapaDireccionDialog.seleccionar(
                                btnMapa.getScene().getWindow(), latitudSeleccionada, longitudSeleccionada)
                        .ifPresent(c -> {
                            latitudSeleccionada = c.latitud();
                            longitudSeleccionada = c.longitud();
                            btnMapa.setText("Coordenadas: " + latitudSeleccionada + ", " + longitudSeleccionada);
                        }));
        cbTipoInforme.setItems(FXCollections.observableArrayList("Pedidos"));
    }

    private void configurarTablaPedidos() {
        colIdPedido.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFecha.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFechaPedido().toString()));
        colEstado.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEstado().name()));
        colTotal.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPrecioTotal().toString()));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));

        colCliente.setCellValueFactory(cellData -> {
            Cliente c = cellData.getValue().getCliente();
            return new SimpleStringProperty(c != null ? c.getNombres() + " " + c.getApellidos() : "N/A");
        });

        colColaborador.setCellValueFactory(cellData -> {
            var colab = cellData.getValue().getColaborador();
            return new SimpleStringProperty(colab != null ? colab.getNombres() + " " + colab.getApellidos() : "N/A");
        });
    }

    private void cargarDatosPedidos() {
        List<Pedido> pedidos = pedidoService.obtenerTodos();
        pedidosObservableList = FXCollections.observableArrayList(pedidos);
        tblPedidos.setItems(pedidosObservableList);
    }

    @FXML
    void nuevoPedido(ActionEvent event) {
        abrirFormulario("nuevo-pedido-view.fxml", "Nuevo pedido");
    }

    @FXML
    void actualizarEstado(ActionEvent event) {
        Pedido pedidoSeleccionado = tblPedidos.getSelectionModel().getSelectedItem();
        if (pedidoSeleccionado != null) {
            var estados = java.util.List.of(ni.edu.uam.proyectofinalpae.model.EstadoPedido.values());
            ChoiceDialog<ni.edu.uam.proyectofinalpae.model.EstadoPedido> dialogo =
                    new ChoiceDialog<>(pedidoSeleccionado.getEstado(), estados);
            dialogo.setHeaderText("Actualizar pedido #" + pedidoSeleccionado.getId());
            dialogo.showAndWait().ifPresent(estado -> {
                pedidoService.actualizarEstado(pedidoSeleccionado.getId(), estado);
                cargarDatosPedidos();
            });
        } else {
            mostrarAlerta(Alert.AlertType.WARNING, "Selección requerida", "Debe seleccionar un pedido de la tabla.");
        }
    }

    @FXML
    void guardarCliente(ActionEvent event) {
        try {
            if (txtPrimerNombre.getText().isEmpty() || txtApellido.getText().isEmpty() || txtEmail.getText().isEmpty()) {
                mostrarAlerta(Alert.AlertType.WARNING, "Campos Incompletos", "El nombre, apellido y correo son obligatorios.");
                return;
            }
            Cliente nuevoCliente = new Cliente();
            nuevoCliente.setUsername(txtRegUsuario.getText());
            nuevoCliente.setNombres(txtPrimerNombre.getText());
            nuevoCliente.setApellidos(txtApellido.getText());
            nuevoCliente.setCorreo(txtEmail.getText());
            nuevoCliente.setTelefono(txtTelefono.getText());

            if (!txtDireccion.getText().isEmpty()) {
                Direccion direccion = new Direccion();
                direccion.setNombre(txtDireccion.getText());
                direccion.setLatitud(latitudSeleccionada);
                direccion.setLongitud(longitudSeleccionada);
                // El método agregarDireccion hace el enlace bidireccional (this)
                nuevoCliente.agregarDireccion(direccion);
            }

<<<<<<< HEAD
            clienteService.registrar(nuevoCliente, txtRegPassword.getText());
=======
            Stage loginStage = new Stage();
            loginStage.setTitle("Inicio de Sesión");
            loginStage.setScene(new Scene(root));
            loginStage.setMaximized(true);
            loginStage.show();
>>>>>>> origin/main

            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Cliente registrado. Revisa el correo para verificarlo.");
            limpiarFormularioCliente(null);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error al guardar", "Ocurrió un error en la base de datos: " + e.getMessage());
        }
    }

    @FXML
    void limpiarFormularioCliente(ActionEvent event) {
        txtRegUsuario.clear();
        txtRegPassword.clear();
        txtPrimerNombre.clear();
        txtApellido.clear();
        txtEmail.clear();
        txtTelefono.clear();
        txtDireccion.clear();
        latitudSeleccionada = null;
        longitudSeleccionada = null;
        if (btnMapa != null) btnMapa.setText("Elegir en OpenStreetMap");
    }

    @FXML
    void generarInforme(ActionEvent event) {
        if (dpFechaInicio.getValue() == null || dpFechaFinal.getValue() == null || cbTipoInforme.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Faltan Parámetros", "Debe seleccionar las fechas y el tipo de informe.");
            return;
        }

        if (dpFechaFinal.getValue().isBefore(dpFechaInicio.getValue())) {
            mostrarAlerta(Alert.AlertType.WARNING, "Rango inválido", "La fecha final es anterior a la inicial.");
            return;
        }
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar informe de pedidos");
        selector.setInitialFileName("pedidos.pdf");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File destino = selector.showSaveDialog(tblPedidos.getScene().getWindow());
        if (destino == null) return;
        try {
            new ReporteService().exportarPedidos(dpFechaInicio.getValue(), dpFechaFinal.getValue(), destino.toPath());
            mostrarAlerta(Alert.AlertType.INFORMATION, "Informe generado", "PDF guardado en: " + destino);
        } catch (Exception ex) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error JasperReports", ex.getMessage());
        }
    }

    @FXML private void abrirVentas(ActionEvent e) {
        abrirFormulario("ventas-view.fxml", "Historial de ventas");
    }

    @FXML private void abrirColaboradores(ActionEvent e) {
        abrirFormulario("nuevo-colaborador-view.fxml", "Nuevo colaborador");
    }

    @FXML private void abrirProductos(ActionEvent e) {
        abrirFormulario("producto-view.fxml", "Productos");
    }


    private void abrirFormulario(String archivo, String titulo) {
        try {
            java.net.URL ruta = getClass().getResource(
                    "/ni/edu/uam/proyectofinalpae/fxml/" + archivo
            );

            if (ruta == null) {
                throw new IllegalArgumentException(
                        "Recurso inexistente: " + archivo
                );
            }

            javafx.fxml.FXMLLoader loader =
                    new javafx.fxml.FXMLLoader(ruta);

            javafx.scene.Parent pantalla = loader.load();

            javafx.stage.Stage ventana =
                    new javafx.stage.Stage();

            ventana.initOwner(
                    tblPedidos.getScene().getWindow()
            );

            ventana.initModality(
                    javafx.stage.Modality.WINDOW_MODAL
            );

            ventana.setTitle(titulo);

            javafx.scene.Scene escena =
                    new javafx.scene.Scene(pantalla, 900, 650);

            ni.edu.uam.proyectofinalpae.util.SceneManager
                    .aplicarEstilos(escena);

            ventana.setScene(escena);
            ventana.showAndWait();

            cargarDatosPedidos();

        } catch (Exception ex) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "No se pudo abrir el formulario",
                    ex.getMessage()
            );
        }
    }


    public void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }


    @FXML
    private void cerrarSesion() {
        ni.edu.uam.proyectofinalpae.util.SceneManager
                .volverLogin();
    }

}