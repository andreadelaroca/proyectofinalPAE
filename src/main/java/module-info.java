module ni.edu.uam.proyectofinalpae {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires java.sql;
    requires java.naming;

    opens ni.edu.uam.proyectofinalpae to javafx.fxml;
    exports ni.edu.uam.proyectofinalpae;
    exports ni.edu.uam.proyectofinalpae.application;
    opens ni.edu.uam.proyectofinalpae.application to javafx.fxml;
    exports ni.edu.uam.proyectofinalpae.controller;
    opens ni.edu.uam.proyectofinalpae.controller to javafx.fxml;
}