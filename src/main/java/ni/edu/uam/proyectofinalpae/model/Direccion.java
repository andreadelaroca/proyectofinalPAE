package ni.edu.uam.proyectofinalpae.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "direcciones") @Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Direccion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;
}
