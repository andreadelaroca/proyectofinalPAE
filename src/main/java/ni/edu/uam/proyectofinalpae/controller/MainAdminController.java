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

        cbTipoInforme.setItems(FXCollections.observableArrayList("Beneficios", "Demanda de Productos", "Mejores Clientes"));
    }

    private void configurarTablaPedidos() {
        colIdPedido.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFecha.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFechaPedido().toString()));
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
        mostrarAlerta(Alert.AlertType.INFORMATION, "Nuevo Pedido", "Funcionalidad de abrir formulario de pedido en construcción.");
    }

    @FXML
    void actualizarEstado(ActionEvent event) {
        Pedido pedidoSeleccionado = tblPedidos.getSelectionModel().getSelectedItem();
        if (pedidoSeleccionado != null) {
            // TODO: Implementar lógica de cambio de estado cuando se agregue al modelo
            mostrarAlerta(Alert.AlertType.INFORMATION, "Actualizar Estado", "Falta agregar el campo 'estado' en la entidad Pedido.");
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
            nuevoCliente.setNombres(txtPrimerNombre.getText());
            nuevoCliente.setApellidos(txtApellido.getText());
            nuevoCliente.setCorreo(txtEmail.getText());
            nuevoCliente.setTelefono(txtTelefono.getText());

            if (!txtDireccion.getText().isEmpty()) {
                Direccion direccion = new Direccion();
                direccion.setNombre(txtDireccion.getText());
                // El método agregarDireccion hace el enlace bidireccional (this)
                nuevoCliente.agregarDireccion(direccion);
            }

            clienteService.guardar(nuevoCliente);

            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Cliente registrado correctamente.");
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
    }

    @FXML
    void generarInforme(ActionEvent event) {
        if (dpFechaInicio.getValue() == null || dpFechaFinal.getValue() == null || cbTipoInforme.getValue() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Faltan Parámetros", "Debe seleccionar las fechas y el tipo de informe.");
            return;
        }

        // TODO: Implementar consultas HQL personalizadas en los Repositories para obtener
        mostrarAlerta(Alert.AlertType.INFORMATION, "Generar Informe",
                "Generando informe de " + cbTipoInforme.getValue() + " del " + dpFechaInicio.getValue() + " al " + dpFechaFinal.getValue());
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}