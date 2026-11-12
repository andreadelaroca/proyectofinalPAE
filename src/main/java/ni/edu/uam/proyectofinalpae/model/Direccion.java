package ni.edu.uam.proyectofinalpae.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "direcciones") @Getter @Setter @AllArgsConstructor @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Direccion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;
}
