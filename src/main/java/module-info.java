module ni.edu.uam.proyectofinalpae {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires jakarta.persistence;

    opens ni.edu.uam.proyectofinalpae to javafx.fxml;
    exports ni.edu.uam.proyectofinalpae;
    exports ni.edu.uam.proyectofinalpae.application;
    opens ni.edu.uam.proyectofinalpae.application to javafx.fxml;
    exports ni.edu.uam.proyectofinalpae.controller;
    opens ni.edu.uam.proyectofinalpae.controller to javafx.fxml;
}