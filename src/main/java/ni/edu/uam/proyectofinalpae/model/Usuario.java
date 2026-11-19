package ni.edu.uam.proyectofinalpae.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.service.spi.InjectService;

@MappedSuperclass @Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String username;

    @Column(nullable = false) //TODO: hashing
    private String password;

    @Column(nullable = false, length = 40)
    private String nombres;

    @Column(nullable = false, length = 40)
    private String apellidos;

    @Column(nullable = false, length = 60)
    private String correo;

    @Column(nullable = false, length = 20)
    private String telefono;
}
