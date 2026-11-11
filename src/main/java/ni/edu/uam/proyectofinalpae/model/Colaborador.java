package ni.edu.uam.proyectofinalpae.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "colaborador")
public class Colaborador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String nombres;

    @Column(nullable = false, length = 40)
    private String apellidos;

    @Column(nullable = false, length = 60)
    private String correo;

    @Column(nullable = false, length = 120)
    private String direccion;

    @Column(nullable = false, length = 20)
    private String telefono;

    @OneToMany(mappedBy = "colaborador", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pedido> pedidos = new ArrayList<>();
}
