package ni.edu.uam.proyectofinalpae.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "pedidos") @Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Pedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fechaPedido;

    @Column(nullable = false)
    private LocalDate fechaEntrega;

    @Column(nullable = false, length = 150)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioTotal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private EstadoPedido estado = EstadoPedido.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_colaborador")
    private Colaborador colaborador;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_direccion_entrega")
    private Direccion direccionEntrega;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();

    public Pedido(LocalDate fechaPedido, LocalDate fechaEntrega, String descripcion) {
        this.fechaPedido = fechaPedido;
        this.fechaEntrega = fechaEntrega;
        this.descripcion = descripcion;
    }

    public void agregarDetalle(DetallePedido detalle) {
        detalle.setPedido(this);
        detalle.calcularSubtotal();
        detalles.add(detalle);
        recalcularTotal();
    }

    public void removerDetalle(DetallePedido detalle) {
        detalles.remove(detalle);
        detalle.setPedido(null);
        recalcularTotal();
    }

    public void recalcularTotal() {
        this.precioTotal = detalles.stream()
                .map(DetallePedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}