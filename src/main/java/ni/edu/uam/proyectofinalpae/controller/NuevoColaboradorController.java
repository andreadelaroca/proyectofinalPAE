package ni.edu.uam.proyectofinalpae.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalpae.model.Colaborador;
import ni.edu.uam.proyectofinalpae.service.ColaboradorService;
import ni.edu.uam.proyectofinalpae.util.Async;
import ni.edu.uam.proyectofinalpae.util.Validador;

public class NuevoColaboradorController {

    @FXML private TextField txtNombres;
    @FXML private TextField txtApellidos;
    @FXML private TextField txtUsername;
    @FXML private TextField txtTelefono;
    @FXML private PasswordField pfPassword;
    @FXML private PasswordField pfConfirmarPassword;
    @FXML private TextField txtCorreo;
    @FXML private TextArea txtDireccion;

    @FXML private Label lblMensaje;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardar;

    private final ColaboradorService colaboradorService = new ColaboradorService();
    private Colaborador colaboradorCreado;

    private record Resultado(Colaborador colaborador, String codigo) {}

    @FXML
    public void initialize() {
        lblMensaje.setText("");
        btnCancelar.setOnAction(e -> cerrarVentana());
        btnGuardar.setOnAction(e -> guardarColaborador());
    }

    public Colaborador getColaboradorCreado() {
        return colaboradorCreado;
    }

    private void guardarColaborador() {
        lblMensaje.setText("");

        String nombres = txtNombres.getText().trim();
        String apellidos = txtApellidos.getText().trim();
        String username = txtUsername.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String correo = txtCorreo.getText().trim();
        String direccion = txtDireccion.getText().trim();
        String password = pfPassword.getText();

        String error = Validador.validarCuenta(nombres, apellidos, username, telefono, correo,
                password, pfConfirmarPassword.getText());
        if (error == null && direccion.isEmpty()) error = "Ingrese la dirección del colaborador.";
        if (error == null && direccion.length() > 120) error = "La dirección no puede superar los 120 caracteres.";
        if (error != null) {
            lblMensaje.setText(error);
            return;
        }

        btnGuardar.setDisable(true);

        Async.ejecutar(() -> {
            Colaborador nuevo = new Colaborador();
            nuevo.setNombres(nombres);
            nuevo.setApellidos(apellidos);
            nuevo.setUsername(username);
            nuevo.setCorreo(correo);
            nuevo.setTelefono(telefono);
            nuevo.setDireccion(direccion);

            String codigo = colaboradorService.registrar(nuevo, password);
            return new Resultado(nuevo, codigo);
        }, resultado -> {
            btnGuardar.setDisable(false);
            colaboradorCreado = resultado.colaborador();
            VerificacionController.mostrar(btnGuardar.getScene().getWindow(), username, correo, resultado.codigo());
            cerrarVentana();
        }, e -> {
            btnGuardar.setDisable(false);
            lblMensaje.setText(Async.mensaje(e));
        });
    }

    private void cerrarVentana() {
        ((Stage) btnCancelar.getScene().getWindow()).close();
    }
}