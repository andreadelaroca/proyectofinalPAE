package ni.edu.uam.proyectofinalpae.controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalpae.model.Pedido;
import ni.edu.uam.proyectofinalpae.service.PedidoService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javafx.concurrent.Task;
import java.util.List;

public class GestionPedidosController {

    @FXML private Button btnNuevoPedido;
    @FXML private Button btnActualizarEstado;
    @FXML private TableView<Pedido> tblPedidos;
    @FXML private TableColumn<Pedido, Long> colIdPedido;
    @FXML private TableColumn<Pedido, LocalDate> colFechaCompra;
    @FXML private TableColumn<Pedido, String> colEstado;
    @FXML private TableColumn<Pedido, BigDecimal> colTotal;
    @FXML private TableColumn<Pedido, String> colDescripcion;
    @FXML private TableColumn<Pedido, String> colUsuarioCliente;
    @FXML private TableColumn<Pedido, String> colUsuarioColaborador;
    @FXML private Button btnRefrescar;
    @FXML private TextField txtBuscar;

    private FilteredList<Pedido> listaFiltrada;

    private final PedidoService pedidoService = new PedidoService();
    private final ObservableList<Pedido> listaPedidos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarColumnas();
        cargarPedidos();

        // El botón solo se activa cuando hay un pedido seleccionado en la tabla
        btnActualizarEstado.disableProperty().bind(
                tblPedidos.getSelectionModel().selectedItemProperty().isNull()
        );

        btnNuevoPedido.setOnAction(e -> abrirNuevoPedido());
        btnActualizarEstado.setOnAction(e -> actualizarEstadoPedido());
        btnRefrescar.setOnAction(e -> cargarPedidos());

        listaFiltrada = new FilteredList<>(listaPedidos, p -> true);
        tblPedidos.setItems(listaFiltrada);
        configurarFiltro();
    }

    private void configurarColumnas() {
        colIdPedido.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getId()));
        colFechaCompra.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getFechaPedido()));
        colEstado.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getEstado()));
        colTotal.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getPrecioTotal()));
        colDescripcion.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getDescripcion()));

        // Muestra el username; puedes usar getNombres() + " " + getApellidos() si prefieres nombre completo
        colUsuarioCliente.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getCliente().getUsername()));
        colUsuarioColaborador.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getColaborador().getUsername()));
    }

    private void cargarPedidos() {
        tblPedidos.setPlaceholder(new Label("Cargando pedidos..."));
        btnRefrescar.setDisable(true);

        Task<List<Pedido>> tarea = new Task<>() {
            @Override
            protected List<Pedido> call() {
                return pedidoService.obtenerTodos();
            }
        };

        tarea.setOnSucceeded(e -> {
            listaPedidos.setAll(tarea.getValue());
            tblPedidos.setPlaceholder(new Label("No hay pedidos registrados."));
            btnRefrescar.setDisable(false);
        });

        tarea.setOnFailed(e -> {
            tblPedidos.setPlaceholder(new Label("Error al cargar los pedidos."));
            btnRefrescar.setDisable(false);
            tarea.getException().printStackTrace();
        });

        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }
    private void actualizarEstadoPedido() {
        Pedido seleccionado = tblPedidos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        List<String> estados = List.of("Pendiente", "En proceso", "Entregado", "Cancelado");
        ChoiceDialog<String> dialogo = new ChoiceDialog<>(seleccionado.getEstado(), estados);
        dialogo.setTitle("Actualizar Estado");
        dialogo.setHeaderText("Pedido #" + seleccionado.getId() + " - " + seleccionado.getCliente().getNombres());
        dialogo.setContentText("Seleccione el nuevo estado:");

        dialogo.showAndWait().ifPresent(nuevoEstado -> {
            // Si eligió el mismo estado actual, no envía ninguna consulta a PostgreSQL
            if (nuevoEstado.equals(seleccionado.getEstado())) return;

            try {
                // 1 sola consulta UPDATE a la base de datos
                pedidoService.actualizarEstado(seleccionado.getId(), nuevoEstado);

                // Refresca la fila en memoria sin volver a consultar toda la tabla
                seleccionado.setEstado(nuevoEstado);
                tblPedidos.refresh();
            } catch (Exception e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error",
                        "No se pudo actualizar el estado: " + e.getMessage());
            }
        });
    }

    private void abrirNuevoPedido() {
        try {
            // Ajusta el nombre del FXML al de tu formulario de registro de pedidos
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ni/edu/uam/proyectofinalpae/fxml/nuevo-pedido-view.fxml")
            );
            Parent root = loader.load();

            Stage modal = new Stage();
            modal.setTitle("BunBun Hub - Nuevo Pedido");
            modal.setScene(new Scene(root));
            modal.initOwner(btnNuevoPedido.getScene().getWindow());
            modal.initModality(Modality.WINDOW_MODAL);
            modal.setResizable(false); // Bloquea el redimensionamiento
            modal.showAndWait();

            // Recarga la tabla al cerrar la ventana modal para mostrar el nuevo pedido
            cargarPedidos();
        } catch (IOException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error",
                    "No se pudo abrir la ventana de nuevo pedido.");
            e.printStackTrace();
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void configurarFiltro() {
        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> {
            String filtro = (nuevo == null) ? "" : nuevo.toLowerCase().trim();
            listaFiltrada.setPredicate(pedido -> {
                if (filtro.isEmpty()) return true;

                String idTexto = pedido.getId() != null ? String.valueOf(pedido.getId()) : "";
                String cliente = (pedido.getCliente() != null)
                        ? (pedido.getCliente().getNombres() + " " + pedido.getCliente().getApellidos() + " " + pedido.getCliente().getUsername()).toLowerCase()
                        : "";
                String colaborador = (pedido.getColaborador() != null)
                        ? (pedido.getColaborador().getNombres() + " " + pedido.getColaborador().getApellidos()).toLowerCase()
                        : "";
                String estado = pedido.getEstado() != null ? pedido.getEstado().toLowerCase() : "";

                return idTexto.contains(filtro)
                        || cliente.contains(filtro)
                        || colaborador.contains(filtro)
                        || estado.contains(filtro);
            });
        });
    }
}