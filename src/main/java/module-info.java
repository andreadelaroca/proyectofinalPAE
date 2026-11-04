module ni.edu.uam.proyectofinalpae {
    requires javafx.controls;
    requires javafx.fxml;


    opens ni.edu.uam.proyectofinalpae to javafx.fxml;
    exports ni.edu.uam.proyectofinalpae;
}