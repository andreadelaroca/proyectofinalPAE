module ni.edu.uam.proyectofinalpae {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires java.naming;
    requires java.sql;
    requires bcrypt;
    requires net.sf.jasperreports.core;
    requires jakarta.mail;
    requires javafx.web;

    opens ni.edu.uam.proyectofinalpae to javafx.fxml;
    exports ni.edu.uam.proyectofinalpae;
    exports ni.edu.uam.proyectofinalpae.application;
    opens ni.edu.uam.proyectofinalpae.application to javafx.fxml;
    exports ni.edu.uam.proyectofinalpae.controller;
    opens ni.edu.uam.proyectofinalpae.controller to javafx.fxml;
    opens ni.edu.uam.proyectofinalpae.model;
}