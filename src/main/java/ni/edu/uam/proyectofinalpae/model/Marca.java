package ni.edu.uam.proyectofinalpae.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "marcas") @Getter @Setter @AllArgsConstructor @NoArgsConstructor(access =  AccessLevel.PROTECTED)
public class Marca {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String nombre;
}
