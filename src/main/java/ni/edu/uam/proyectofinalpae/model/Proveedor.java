package ni.edu.uam.proyectofinalpae.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "proveedores") @Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access =  AccessLevel.PROTECTED)
public class Proveedor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String nombre;
}
