package ni.edu.uam.proyectofinalpae.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity @Table(name = "producto")
public class Producto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 70)
    private String nombre;

    @Column(nullable = false, length = 30)
    private String marca;

    @Column(nullable = false, length = 120)
    private String descripcion;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, length = 6)
    private String color; //TODO: implementar colorpicker en el controller
}
