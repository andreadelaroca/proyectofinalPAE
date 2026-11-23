package ni.edu.uam.proyectofinalpae.model;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "clientes") @PrimaryKeyJoinColumn(name = "id") @Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Cliente extends Usuario {
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Direccion> direcciones = new ArrayList<>();

    @OneToMany(mappedBy = "cliente")
    private List<Pedido> pedidos = new ArrayList<>();

    @OneToMany(mappedBy = "cliente")
    private List<Venta> ventas = new ArrayList<>();

    public void agregarDireccion(Direccion direccion) {
        direcciones.add(direccion);
        direccion.setCliente(this);
    }

    public void removerDireccion(Direccion direccion) {
        direcciones.remove(direccion);
        direccion.setCliente(null);
    }
}