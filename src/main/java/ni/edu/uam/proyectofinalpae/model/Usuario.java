package ni.edu.uam.proyectofinalpae.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.service.spi.InjectService;

import java.time.LocalDateTime;

@MappedSuperclass @Getter @Setter @AllArgsConstructor @NoArgsConstructor @Inheritance(strategy = InheritanceType.JOINED)
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String username;

    @Column(nullable = false, name = "passwordHash", length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 40)
    private String nombres;

    @Column(nullable = false, length = 40)
    private String apellidos;

    @Column(nullable = false, length = 60)
    private String correo;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(nullable = false)
    private boolean correoVerificado = false;

    @Column(length = 100)
    private String tokenVerificacion;

    private LocalDateTime tokenExpiracion;

    @Column(nullable = false)
    private boolean activo = true;
}
