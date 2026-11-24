module ni.edu.uam.proyectofinalpae {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires java.naming;
    requires java.sql;
    requires bcrypt;
    requires java.desktop;
    requires jakarta.mail;
    requires javafx.web;
    requires net.sf.jasperreports.core;


    // JasperReports 7 (módulos automáticos) y sus dependencias en tiempo de ejecución.
    // commons-logging y beanutils2 son módulos explícitos: si nadie los requiere, no se cargan
    // y JasperReports falla con NoClassDefFoundError al iniciar.
    requires net.sf.jasperreports.pdf;
    requires net.sf.jasperreports.jdt;
    requires net.sf.jasperreports.fonts;
    requires org.apache.commons.logging;
    requires org.apache.commons.beanutils2;

    // Las filas del reporte se leen por reflexión (JRBeanCollectionDataSource / TableView)
    opens ni.edu.uam.proyectofinalpae.report;
    opens ni.edu.uam.proyectofinalpae to javafx.fxml;
    exports ni.edu.uam.proyectofinalpae;
    exports ni.edu.uam.proyectofinalpae.application;
    opens ni.edu.uam.proyectofinalpae.application to javafx.fxml;
    exports ni.edu.uam.proyectofinalpae.controller;
    opens ni.edu.uam.proyectofinalpae.controller to javafx.fxml;
    opens ni.edu.uam.proyectofinalpae.model;
}