package ni.edu.uam.proyectofinalpae.models.agenda;

import jakarta.persistence.*;

@Entity
@Table(name = "Clientes", schema = "Agenda")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    @Column(name = "nombres", nullable = false, length = 50)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 50)
    private String apellidos;

    @Column(name = "telefono", length = 15)
    private String telefono;

    @Column(name = "notas_sensibilidad_dermica", columnDefinition = "TEXT")
    private String notasSensibilidadDermica;

    public Cliente() {}

    public Integer getIdCliente() { return idCliente; }
    public void setIdCliente(Integer idCliente) { this.idCliente = idCliente; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getNotasSensibilidadDermica() { return notasSensibilidadDermica; }
    public void setNotasSensibilidadDermica(String notasSensibilidadDermica) { this.notasSensibilidadDermica = notasSensibilidadDermica; }
}