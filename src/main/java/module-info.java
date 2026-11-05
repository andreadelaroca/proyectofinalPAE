module ni.edu.uam.proyectofinalpae {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;

    opens ni.edu.uam.proyectofinalpae to javafx.fxml;
    opens ni.edu.uam.proyectofinalpae.models.seguridad to org.hibernate.orm.core;
    opens ni.edu.uam.proyectofinalpae.models.inventario to org.hibernate.orm.core;
    opens ni.edu.uam.proyectofinalpae.models.agenda to org.hibernate.orm.core;

    exports ni.edu.uam.proyectofinalpae;
    exports ni.edu.uam.proyectofinalpae.models.seguridad;
    exports ni.edu.uam.proyectofinalpae.models.inventario;
    exports ni.edu.uam.proyectofinalpae.models.agenda;
    exports ni.edu.uam.proyectofinalpae.util;
}