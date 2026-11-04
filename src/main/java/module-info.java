module ni.edu.uam.proyectofinalpae {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.naming;
    requires java.sql;
    requires org.hibernate.orm.core;
    requires jakarta.persistence;

    opens ni.edu.uam.proyectofinalpae to javafx.fxml;
    opens ni.edu.uam.proyectofinalpae.model.inventario to org.hibernate.orm.core;
    opens ni.edu.uam.proyectofinalpae.model.agenda to org.hibernate.orm.core;
    opens ni.edu.uam.proyectofinalpae.model.seguridad to org.hibernate.orm.core;

    exports ni.edu.uam.proyectofinalpae;
    exports ni.edu.uam.proyectofinalpae.model.inventario;
    exports ni.edu.uam.proyectofinalpae.model.agenda;
    exports ni.edu.uam.proyectofinalpae.model.seguridad;
}