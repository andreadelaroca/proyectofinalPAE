package ni.edu.uam.proyectofinalpae.model;

public enum EstadoVenta {
    COMPLETADA("Completada"), ANULADA("Anulada");

    private final String etiqueta;

    EstadoVenta(String etiqueta) { this.etiqueta = etiqueta; }

    @Override
    public String toString() { return etiqueta; }
}