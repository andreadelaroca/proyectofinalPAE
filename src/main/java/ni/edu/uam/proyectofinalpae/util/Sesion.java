package ni.edu.uam.proyectofinalpae.util;

import ni.edu.uam.proyectofinalpae.model.Colaborador;
import ni.edu.uam.proyectofinalpae.model.Usuario;

public final class Sesion {
    private static Usuario actual;

    private Sesion() {}

    public static void iniciar(Usuario usuario) { actual = usuario; }
    public static void cerrar() { actual = null; }
    public static Usuario getUsuario() { return actual; }

    public static Colaborador getColaborador() {
        return actual instanceof Colaborador c ? c : null;
    }
}