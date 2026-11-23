package ni.edu.uam.proyectofinalpae.config;

import ni.edu.uam.proyectofinalpae.model.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class HibernateUtil {
    private static SessionFactory factory;
    private HibernateUtil() {}
    private static SessionFactory crear() {
        try {
            return new Configuration().addProperties(cargar())
                    .addAnnotatedClass(Usuario.class)
                    .addAnnotatedClass(Cliente.class)
                    .addAnnotatedClass(Colaborador.class)
                    .addAnnotatedClass(Direccion.class)
                    .addAnnotatedClass(Marca.class)
                    .addAnnotatedClass(Producto.class)
                    .addAnnotatedClass(Pedido.class)
                    .addAnnotatedClass(DetallePedido.class)
                    .addAnnotatedClass(Venta.class)
                    .addAnnotatedClass(DetalleVenta.class)
                    .buildSessionFactory();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo iniciar Hibernate. Revise PostgreSQL y hibernate.properties",e);
        }
    }

    private static java.util.Properties cargar() {
        try (var archivo = HibernateUtil.class.getResourceAsStream("/hibernate.properties")) {
            if (archivo == null) throw new IllegalStateException("Falta hibernate.properties");
            var propiedades = new java.util.Properties();
            propiedades.load(archivo);
            asignarDesdeEntorno(propiedades, "DB_URL", "hibernate.connection.url");
            asignarDesdeEntorno(propiedades, "DB_USER", "hibernate.connection.username");
            asignarDesdeEntorno(propiedades, "DB_PASSWORD", "hibernate.connection.password");
            return propiedades;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Error leyendo configuración",e);
        }
    }
    private static void asignarDesdeEntorno(java.util.Properties p, String variable, String propiedad) {
        String valor = System.getenv(variable);
        if (valor == null) valor = System.getProperty(variable);
        if (valor != null) p.setProperty(propiedad, valor);
    }

    public static synchronized SessionFactory factory() {
        if (factory == null || factory.isClosed()) factory = crear();
        return factory;
    }

    public static synchronized void cerrar() {
        if (factory != null && !factory.isClosed()) factory.close();
    }
}