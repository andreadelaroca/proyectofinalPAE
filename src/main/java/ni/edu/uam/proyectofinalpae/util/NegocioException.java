package ni.edu.uam.proyectofinalpae.util;

//Mensaje de reglas de negocio
public class NegocioException extends RuntimeException {
    public NegocioException(String message) {
        super(message);
    }
}
