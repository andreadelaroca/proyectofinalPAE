package ni.edu.uam.proyectofinalpae.model;

public enum EstadoPedido {
    PENDIENTE("Pendiente"), EN_PROCESO("En proceso"), LISTO("Listo"),
    ENTREGADO("Entregado"), CANCELADO("Cancelado");

    private final String etiqueta;

    EstadoPedido(String etiqueta) { this.etiqueta = etiqueta; }

    /** Lo que ve el usuario en tablas y diálogos. En la BD se guarda name(). */
    @Override
    public String toString() { return etiqueta; }
}