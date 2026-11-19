package ni.edu.uam.proyectofinalpae.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "colaboradores") @Getter @Setter @PrimaryKeyJoinColumn(name = "id") @NoArgsConstructor
public class Colaborador extends Usuario {
    @Column(nullable = false, length = 120)
    private String direccion;

    @OneToMany(mappedBy = "colaborador")
    private List<Pedido> pedidos = new ArrayList<>();

    @OneToMany(mappedBy = "colaborador")
    private List<Venta> ventas = new ArrayList<>();
}
