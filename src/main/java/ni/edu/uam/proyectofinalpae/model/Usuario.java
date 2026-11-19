package ni.edu.uam.proyectofinalpae.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(name = "uk_usuario_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_usuario_correo", columnNames = "correo")
})
@Inheritance(strategy = InheritanceType.JOINED)
@Getter @Setter @NoArgsConstructor
public abstract class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 40)
    private String nombres;

    @Column(nullable = false, length = 40)
    private String apellidos;

    @Column(nullable = false, length = 60)
    private String correo;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(name = "correo_verificado", nullable = false)
    private boolean correoVerificado = false;

    @Column(name = "token_verificacion", length = 100)
    private String tokenVerificacion;

    @Column(name = "token_expiracion")
    private LocalDateTime tokenExpiracion;

    @Column(nullable = false)
    private boolean activo = true;
}