module ni.edu.uam.proyectofinalpae {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.persistence;


    opens ni.edu.uam.proyectofinalpae to javafx.fxml;
    exports ni.edu.uam.proyectofinalpae;
}