package ni.edu.uam.proyectofinalpae.report;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import ni.edu.uam.proyectofinalpae.model.Cliente;
import ni.edu.uam.proyectofinalpae.model.Colaborador;
import ni.edu.uam.proyectofinalpae.model.DetalleVenta;
import ni.edu.uam.proyectofinalpae.model.EstadoPedido;
import ni.edu.uam.proyectofinalpae.model.EstadoVenta;
import ni.edu.uam.proyectofinalpae.model.Pedido;
import ni.edu.uam.proyectofinalpae.model.Venta;
import ni.edu.uam.proyectofinalpae.report.DisenoReporte.Columna;
import ni.edu.uam.proyectofinalpae.report.DisenoReporte.Dato;
import ni.edu.uam.proyectofinalpae.service.PedidoService;
import ni.edu.uam.proyectofinalpae.util.Tx;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Reportes con JasperReports. Cada método {@code generar...} consulta los datos,
 * arma el diseño ({@link DisenoReporte}) y devuelve el {@link JasperPrint} listo
 * para la vista previa; {@link #exportarPdf} lo guarda como PDF.
 */
public class ReporteService {

    // ------------------------------------------------------------ Pedidos (admin)

    public JasperPrint generarPedidos(LocalDate desde, LocalDate hasta) throws JRException {
        validarRango(desde, hasta);
        List<Pedido> pedidos = Tx.read(s -> s.createQuery(
                        "SELECT p FROM Pedido p JOIN FETCH p.cliente " +
                                "WHERE p.fechaPedido BETWEEN :desde AND :hasta ORDER BY p.fechaPedido, p.id", Pedido.class)
                .setParameter("desde", desde).setParameter("hasta", hasta).list());

        List<FilaPedido> filas = new ArrayList<>();
        for (Pedido p : pedidos) {
            filas.add(new FilaPedido(
                    String.valueOf(p.getId()),
                    formatear(p.getFechaPedido()),
                    formatear(p.getFechaEntrega()),
                    nombreCliente(p.getCliente()),
                    p.getEstado() == null ? "" : p.getEstado().toString(),
                    p.getDescripcion(),
                    dinero(p.getPrecioTotal())));
        }

        BigDecimal total = sumar(pedidos.stream().filter(p -> p.getEstado() != EstadoPedido.CANCELADO)
                .map(Pedido::getPrecioTotal).toList());
        long cancelados = pedidos.stream().filter(p -> p.getEstado() == EstadoPedido.CANCELADO).count();

        JasperDesign d = DisenoReporte.construir("informe_pedidos", "Informe de Pedidos",
                "Pedidos del " + formatear(desde) + " al " + formatear(hasta),
                List.of(),
                List.of(Columna.centro("N.º", "id", 40),
                        Columna.izq("Fecha", "fecha", 70),
                        Columna.izq("Entrega", "entrega", 70),
                        Columna.izq("Cliente", "cliente", 130),
                        Columna.izq("Estado", "estado", 75),
                        Columna.der("Total", "total", 150)),
                List.of(Dato.de("Pedidos en el período", String.valueOf(pedidos.size())),
                        Dato.de("Cancelados", String.valueOf(cancelados)),
                        Dato.total("Monto total", dinero(total))),
                "No hay pedidos registrados en el período seleccionado.");
        return llenar(d, filas);
    }

    // ------------------------------------------------------------- Ventas (admin)

    public JasperPrint generarVentas(LocalDate desde, LocalDate hasta) throws JRException {
        validarRango(desde, hasta);
        List<Venta> ventas = Tx.read(s -> s.createQuery(
                        "SELECT v FROM Venta v LEFT JOIN FETCH v.cliente JOIN FETCH v.colaborador " +
                                "WHERE v.fecha >= :desde AND v.fecha < :hasta ORDER BY v.fecha", Venta.class)
                .setParameter("desde", inicio(desde)).setParameter("hasta", finExclusivo(hasta)).list());

        List<FilaVenta> filas = new ArrayList<>();
        for (Venta v : ventas) {
            filas.add(new FilaVenta(
                    v.getNumeroFactura(),
                    formatear(v.getFecha()),
                    nombreCliente(v.getCliente()),
                    nombreColaborador(v.getColaborador()),
                    v.getMetodoPago() == null ? "" : v.getMetodoPago().toString(),
                    v.getEstado() == null ? "" : v.getEstado().toString(),
                    dinero(v.getTotal())));
        }

        List<Venta> completadas = ventas.stream().filter(v -> v.getEstado() == EstadoVenta.COMPLETADA).toList();
        JasperDesign d = DisenoReporte.construir("informe_ventas", "Informe de Ventas",
                "Ventas del " + formatear(desde) + " al " + formatear(hasta),
                List.of(),
                List.of(Columna.izq("Factura", "factura", 70),
                        Columna.izq("Fecha", "fecha", 85),
                        Columna.izq("Cliente", "cliente", 110),
                        Columna.izq("Atendió", "colaborador", 95),
                        Columna.izq("Pago", "pago", 60),
                        Columna.izq("Estado", "estado", 50),
                        Columna.der("Total", "total", 65)),
                List.of(Dato.de("Ventas completadas", String.valueOf(completadas.size())),
                        Dato.de("Ventas anuladas", String.valueOf(ventas.size() - completadas.size())),
                        Dato.de("IVA recaudado", dinero(sumar(completadas.stream().map(Venta::getImpuesto).toList()))),
                        Dato.total("Total facturado", dinero(sumar(completadas.stream().map(Venta::getTotal).toList())))),
                "No hay ventas registradas en el período seleccionado.");
        return llenar(d, filas);
    }

    // -------------------------------------------------------------------- Factura

    /** La venta debe venir con detalles, productos, cliente y colaborador cargados. */
    public JasperPrint generarFactura(Venta venta) throws JRException {
        if (venta == null || venta.getId() == null) throw new IllegalArgumentException("Venta no válida");
        List<FilaFactura> filas = new ArrayList<>();
        for (DetalleVenta item : venta.getDetalles()) {
            filas.add(new FilaFactura(
                    item.getProducto() == null ? "" : item.getProducto().getNombre(),
                    String.valueOf(item.getCantidad()),
                    dinero(item.getPrecioUnitario()),
                    dinero(item.getSubtotal())));
        }

        List<Dato> datos = new ArrayList<>(List.of(
                Dato.total("Factura", venta.getNumeroFactura()),
                Dato.de("Fecha", formatear(venta.getFecha())),
                Dato.de("Cliente", nombreCliente(venta.getCliente())),
                Dato.de("Atendió", nombreColaborador(venta.getColaborador())),
                Dato.de("Método de pago", venta.getMetodoPago() == null ? "" : venta.getMetodoPago().toString()),
                Dato.de("Estado", venta.getEstado() == null ? "" : venta.getEstado().toString())));

        JasperDesign d = DisenoReporte.construir("factura_venta", "Factura de Venta",
                venta.getEstado() == EstadoVenta.ANULADA ? "FACTURA ANULADA" : null,
                datos,
                List.of(Columna.izq("Producto", "producto", 255),
                        Columna.centro("Cantidad", "cantidad", 70),
                        Columna.der("Precio unitario", "precioUnitario", 105),
                        Columna.der("Subtotal", "subtotal", 105)),
                List.of(Dato.de("Subtotal", dinero(venta.getSubtotal())),
                        Dato.de("IVA (15%)", dinero(venta.getImpuesto())),
                        Dato.total("TOTAL", dinero(venta.getTotal()))),
                "La venta no tiene productos.");
        return llenar(d, filas);
    }

    // -------------------------------------------------- Pedidos de un cliente

    /** Reporte "Mis pedidos" para la pantalla del cliente: solo sus pedidos. */
    public JasperPrint generarPedidosCliente(Cliente cliente) throws JRException {
        if (cliente == null || cliente.getId() == null) throw new IllegalArgumentException("Cliente no válido");
        List<Pedido> pedidos = new PedidoService().obtenerPorCliente(cliente.getId());

        List<FilaPedido> filas = new ArrayList<>();
        for (Pedido p : pedidos) {
            filas.add(new FilaPedido(
                    String.valueOf(p.getId()),
                    formatear(p.getFechaPedido()),
                    formatear(p.getFechaEntrega()),
                    nombreCliente(cliente),
                    p.getEstado() == null ? "" : p.getEstado().toString(),
                    p.getDescripcion(),
                    dinero(p.getPrecioTotal())));
        }

        long activos = pedidos.stream().filter(p -> p.getEstado() != EstadoPedido.ENTREGADO
                && p.getEstado() != EstadoPedido.CANCELADO).count();
        BigDecimal total = sumar(pedidos.stream().filter(p -> p.getEstado() != EstadoPedido.CANCELADO)
                .map(Pedido::getPrecioTotal).toList());

        JasperDesign d = DisenoReporte.construir("pedidos_cliente", "Mis Pedidos",
                "Historial de pedidos al " + formatear(LocalDate.now()),
                List.of(Dato.de("Cliente", nombreCliente(cliente)),
                        Dato.de("Usuario", texto(cliente.getUsername())),
                        Dato.de("Correo", texto(cliente.getCorreo())),
                        Dato.de("Teléfono", texto(cliente.getTelefono()))),
                List.of(Columna.centro("N.º", "id", 40),
                        Columna.izq("Fecha", "fecha", 70),
                        Columna.izq("Entrega", "entrega", 70),
                        Columna.izq("Estado", "estado", 75),
                        Columna.izq("Descripción", "descripcion", 200),
                        Columna.der("Total", "total", 80)),
                List.of(Dato.de("Pedidos realizados", String.valueOf(pedidos.size())),
                        Dato.de("Pedidos en curso", String.valueOf(activos)),
                        Dato.total("Total", dinero(total))),
                "Todavía no tienes pedidos registrados.");
        return llenar(d, filas);
    }

    // ---------------------------------------------------------------- Resumen UI

    /** Filas de resumen para la tabla de la pestaña Informes. */
    public List<FilaResumen> obtenerResumen(String tipo, LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return Tx.read(s -> {
            List<FilaResumen> resumen = new ArrayList<>();
            if ("Pedidos".equals(tipo)) {
                List<Pedido> pedidos = s.createQuery(
                                "SELECT p FROM Pedido p WHERE p.fechaPedido BETWEEN :desde AND :hasta", Pedido.class)
                        .setParameter("desde", desde).setParameter("hasta", hasta).list();
                BigDecimal total = sumar(pedidos.stream().filter(p -> p.getEstado() != EstadoPedido.CANCELADO)
                        .map(Pedido::getPrecioTotal).toList());
                resumen.add(new FilaResumen("Total de pedidos en el rango", String.valueOf(pedidos.size())));
                resumen.add(new FilaResumen("Monto total en pedidos (sin cancelados)", dinero(total)));
            } else if ("Ventas".equals(tipo)) {
                List<Venta> ventas = s.createQuery(
                                "SELECT v FROM Venta v WHERE v.estado = :estado " +
                                        "AND v.fecha >= :desde AND v.fecha < :hasta", Venta.class)
                        .setParameter("estado", EstadoVenta.COMPLETADA)
                        .setParameter("desde", inicio(desde)).setParameter("hasta", finExclusivo(hasta)).list();
                resumen.add(new FilaResumen("Ventas completadas en el rango", String.valueOf(ventas.size())));
                resumen.add(new FilaResumen("Total IVA recaudado", dinero(sumar(ventas.stream().map(Venta::getImpuesto).toList()))));
                resumen.add(new FilaResumen("Monto total facturado", dinero(sumar(ventas.stream().map(Venta::getTotal).toList()))));
            }
            return resumen;
        });
    }

    // ------------------------------------------------------------------ Exportar

    public static Path exportarPdf(JasperPrint reporte, Path destino) throws JRException {
        JasperExportManager.exportReportToPdfFile(reporte, destino.toAbsolutePath().toString());
        return destino;
    }

    // ------------------------------------------------------------------- Ayudas

    private static JasperPrint llenar(JasperDesign diseno, List<?> filas) throws JRException {
        JasperReport compilado = JasperCompileManager.compileReport(diseno);
        // El mapa de parámetros debe ser mutable: JasperReports agrega valores al llenar
        return JasperFillManager.fillReport(compilado, new HashMap<>(), new JRBeanCollectionDataSource(filas));
    }

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static LocalDateTime inicio(LocalDate d) { return d.atStartOfDay(); }

    /** Límite superior exclusivo: incluye todo el día {@code hasta}. */
    private static LocalDateTime finExclusivo(LocalDate d) { return d.plusDays(1).atStartOfDay(); }

    private static String formatear(LocalDate d) { return d == null ? "" : d.format(FMT_FECHA); }

    private static String formatear(LocalDateTime d) { return d == null ? "" : d.format(FMT_FECHA_HORA); }

    private static String texto(String s) { return s == null ? "" : s; }

    private static BigDecimal sumar(List<BigDecimal> valores) {
        return valores.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String dinero(BigDecimal v) {
        return "C$ " + (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String nombreCliente(Cliente c) {
        return c == null ? "Consumidor final" : (texto(c.getNombres()) + " " + texto(c.getApellidos())).trim();
    }

    private static String nombreColaborador(Colaborador c) {
        return c == null ? "" : (texto(c.getNombres()) + " " + texto(c.getApellidos())).trim();
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new IllegalArgumentException("Rango de fechas inválido");
        }
    }

    // ----------------------------------------------- Filas (beans de JasperReports)

    public static final class FilaPedido {
        private final String id, fecha, entrega, cliente, estado, descripcion, total;
        public FilaPedido(String id, String fecha, String entrega, String cliente, String estado,
                          String descripcion, String total) {
            this.id = id; this.fecha = fecha; this.entrega = entrega; this.cliente = cliente;
            this.estado = estado; this.descripcion = descripcion; this.total = total;
        }
        public String getId() { return id; }
        public String getFecha() { return fecha; }
        public String getEntrega() { return entrega; }
        public String getCliente() { return cliente; }
        public String getEstado() { return estado; }
        public String getDescripcion() { return descripcion; }
        public String getTotal() { return total; }
    }

    public static final class FilaVenta {
        private final String factura, fecha, cliente, colaborador, pago, estado, total;
        public FilaVenta(String factura, String fecha, String cliente, String colaborador, String pago,
                         String estado, String total) {
            this.factura = factura; this.fecha = fecha; this.cliente = cliente; this.colaborador = colaborador;
            this.pago = pago; this.estado = estado; this.total = total;
        }
        public String getFactura() { return factura; }
        public String getFecha() { return fecha; }
        public String getCliente() { return cliente; }
        public String getColaborador() { return colaborador; }
        public String getPago() { return pago; }
        public String getEstado() { return estado; }
        public String getTotal() { return total; }
    }

    public static final class FilaFactura {
        private final String producto, cantidad, precioUnitario, subtotal;
        public FilaFactura(String producto, String cantidad, String precioUnitario, String subtotal) {
            this.producto = producto; this.cantidad = cantidad;
            this.precioUnitario = precioUnitario; this.subtotal = subtotal;
        }
        public String getProducto() { return producto; }
        public String getCantidad() { return cantidad; }
        public String getPrecioUnitario() { return precioUnitario; }
        public String getSubtotal() { return subtotal; }
    }

    public static final class FilaResumen {
        private final String concepto, valorTotal;
        public FilaResumen(String concepto, String valorTotal) {
            this.concepto = concepto; this.valorTotal = valorTotal;
        }
        public String getConcepto() { return concepto; }
        public String getValorTotal() { return valorTotal; }
    }
}
