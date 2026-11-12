package ni.edu.uam.proyectofinalpae.model;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "clientes") @Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Cliente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String nombres;

    @Column(nullable = false, length = 40)
    private String apellidos;

    @Column(nullable = false, length = 60)
    private String correo;

    @Column(nullable = false, length = 20)
    private String telefono;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Direccion> direcciones = new ArrayList<>();

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pedido> pedidos = new ArrayList<>();

    public void actualizar(String nombres, String apellidos, String correo, String telefono) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.correo = correo;
        this.telefono = telefono;
    }

    public void agregarPedido(Pedido pedido) {
        pedidos.add(pedido);
        pedido.setCliente(this);
    }

    public void agregarDireccion(Direccion direccion) {
        direcciones.add(direccion);
        direccion.setCliente(this);
    }
}
