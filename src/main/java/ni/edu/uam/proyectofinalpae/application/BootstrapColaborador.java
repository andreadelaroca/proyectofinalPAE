package ni.edu.uam.proyectofinalpae.application;

import ni.edu.uam.proyectofinalpae.config.HibernateUtil;
import ni.edu.uam.proyectofinalpae.model.Colaborador;
import ni.edu.uam.proyectofinalpae.service.ColaboradorService;

public final class BootstrapColaborador {
    private BootstrapColaborador() {}
    private static String obligatorio(String clave) {
        String valor = System.getenv(clave);
        if (valor == null || valor.isBlank()) throw new IllegalStateException("Falta: " + clave);
        return valor;
    }
    public static void main(String[] args) {
        Colaborador c = new Colaborador();
        c.setUsername(obligatorio("BOOTSTRAP_USER"));
        c.setNombres(obligatorio("BOOTSTRAP_NOMBRES"));
        c.setApellidos(obligatorio("BOOTSTRAP_APELLIDOS"));
        c.setCorreo(obligatorio("BOOTSTRAP_EMAIL"));
        c.setTelefono(obligatorio("BOOTSTRAP_TELEFONO"));
        c.setDireccion(obligatorio("BOOTSTRAP_DIRECCION"));
        try {
            new ColaboradorService().registrar(c, obligatorio("BOOTSTRAP_PASSWORD"));
            System.out.println("Colaborador inicial creado; confirma el código recibido por SMTP.");
        } finally {
            HibernateUtil.cerrar();
        }
    }
}