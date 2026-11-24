package ni.edu.uam.proyectofinalpae.report;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.*;
import ni.edu.uam.proyectofinalpae.model.Cliente;
import ni.edu.uam.proyectofinalpae.model.DetalleVenta;
import ni.edu.uam.proyectofinalpae.model.EstadoVenta;
import ni.edu.uam.proyectofinalpae.model.Pedido;
import ni.edu.uam.proyectofinalpae.model.Venta;
import ni.edu.uam.proyectofinalpae.util.Tx;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/** JasperReports: consulta Hibernate y exporta PDF real, sin plantilla JRXML externa. */
public class ReporteService {

    public Path exportarPedidos(LocalDate desde, LocalDate hasta, Path destino) throws JRException {
        validarRango(desde, hasta);
        List<FilaPedido> filas = Tx.read(s -> {
            List<Pedido> pedidos = s.createQuery(
                            "SELECT p FROM Pedido p JOIN FETCH p.cliente " +
                                    "WHERE p.fechaPedido BETWEEN :desde AND :hasta ORDER BY p.fechaPedido", Pedido.class)
                    .setParameter("desde", desde).setParameter("hasta", hasta).list();
            List<FilaPedido> lista = new ArrayList<>();
            for (Pedido p : pedidos) {
                lista.add(new FilaPedido(
                        p.getId(),
                        formatear(p.getFechaPedido()),
                        nombreCliente(p.getCliente()),
                        p.getEstado() == null ? "" : p.getEstado().toString(),
                        dinero(p.getPrecioTotal())
                ));
            }
            return lista;
        });

        JasperReport reporte = JasperCompileManager.compileReport(disenoPedidos());
        JasperPrint relleno = JasperFillManager.fillReport(
                reporte,
                parametros("SUBTITULO", "Pedidos del " + formatear(desde) + " al " + formatear(hasta)),
                new JRBeanCollectionDataSource(filas)
        );
        JasperExportManager.exportReportToPdfFile(relleno, destino.toAbsolutePath().toString());
        return destino;
    }

    public Path exportarVentas(LocalDate desde, LocalDate hasta, Path destino) throws JRException {
        validarRango(desde, hasta);
        List<FilaVenta> filas = Tx.read(s -> {
            List<Venta> ventas = s.createQuery(
                            "SELECT v FROM Venta v LEFT JOIN FETCH v.cliente " +
                                    "WHERE v.fecha >= :desde AND v.fecha < :hasta ORDER BY v.fecha", Venta.class)
                    .setParameter("desde", inicio(desde)).setParameter("hasta", finExclusivo(hasta)).list();
            List<FilaVenta> lista = new ArrayList<>();
            for (Venta v : ventas) {
                lista.add(new FilaVenta(
                        v.getNumeroFactura(),
                        formatear(v.getFecha()),
                        nombreCliente(v.getCliente()),
                        dinero(v.getSubtotal()),
                        dinero(v.getTotal())
                ));
            }
            return lista;
        });

        JasperReport reporte = JasperCompileManager.compileReport(disenoVentas());
        JasperPrint relleno = JasperFillManager.fillReport(
                reporte,
                parametros("SUBTITULO", "Ventas del " + formatear(desde) + " al " + formatear(hasta)),
                new JRBeanCollectionDataSource(filas)
        );
        JasperExportManager.exportReportToPdfFile(relleno, destino.toAbsolutePath().toString());
        return destino;
    }

    /** Devuelve filas de resumen para mostrar en tblInformes antes o después de exportar. */
    public List<FilaResumen> obtenerResumen(String tipo, LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return Tx.read(s -> {
            List<FilaResumen> resumen = new ArrayList<>();
            if ("Pedidos".equals(tipo)) {
                List<Pedido> pedidos = s.createQuery(
                                "SELECT p FROM Pedido p WHERE p.fechaPedido BETWEEN :desde AND :hasta", Pedido.class)
                        .setParameter("desde", desde).setParameter("hasta", hasta).list();
                BigDecimal total = pedidos.stream()
                        .map(Pedido::getPrecioTotal).filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                resumen.add(new FilaResumen("Total de pedidos en el rango", String.valueOf(pedidos.size())));
                resumen.add(new FilaResumen("Monto total en pedidos", dinero(total)));
            } else if ("Ventas".equals(tipo)) {
                List<Venta> ventas = s.createQuery(
                                "SELECT v FROM Venta v WHERE v.estado = :estado " +
                                        "AND v.fecha >= :desde AND v.fecha < :hasta", Venta.class)
                        .setParameter("estado", EstadoVenta.COMPLETADA)
                        .setParameter("desde", inicio(desde)).setParameter("hasta", finExclusivo(hasta)).list();
                BigDecimal total = ventas.stream()
                        .map(Venta::getTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal impuesto = ventas.stream()
                        .map(Venta::getImpuesto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                resumen.add(new FilaResumen("Ventas completadas en el rango", String.valueOf(ventas.size())));
                resumen.add(new FilaResumen("Total IVA recaudado", dinero(impuesto)));
                resumen.add(new FilaResumen("Monto total facturado", dinero(total)));
            }
            return resumen;
        });
    }

    /** Exporta una factura con el detalle persistido de la venta. */
    public Path generarFactura(Venta venta, Path destino) throws JRException {
        if (venta == null || venta.getId() == null) throw new IllegalArgumentException("Venta no válida");
        List<FilaFactura> filas = new ArrayList<>();
        for (DetalleVenta item : venta.getDetalles()) {
            filas.add(new FilaFactura(
                    item.getProducto().getNombre(),
                    String.valueOf(item.getCantidad()),
                    dinero(item.getPrecioUnitario()),
                    dinero(item.getSubtotal())
            ));
        }
        JasperDesign d = baseDesign("factura_venta");
        JRDesignBand titulo = new JRDesignBand();
        titulo.setHeight(115);
        titulo.addElement(textoFijo("BunBun Hub - Factura", 0, 0, 535, 28, 18));
        titulo.addElement(textoFijo("Factura: " + venta.getNumeroFactura(), 0, 32, 535, 18, 11));
        titulo.addElement(textoFijo("Fecha: " + formatear(venta.getFecha()), 0, 53, 535, 18, 10));
        titulo.addElement(textoFijo("Cliente: " + nombreCliente(venta.getCliente()), 0, 75, 535, 18, 10));
        d.setTitle(titulo);

        agregarColumnas(d,
                new String[]{"Producto", "Cantidad", "Precio unitario", "Subtotal"},
                new String[]{"producto", "cantidad", "precioUnitario", "subtotal"},
                new int[]{0, 265, 340, 440},
                new int[]{260, 70, 95, 90});

        JRDesignBand resumen = new JRDesignBand();
        resumen.setHeight(85);
        resumen.addElement(textoFijo("Subtotal: " + dinero(venta.getSubtotal()), 330, 5, 200, 20, 11));
        resumen.addElement(textoFijo("IVA (15%): " + dinero(venta.getImpuesto()), 330, 28, 200, 20, 11));
        resumen.addElement(textoFijo("TOTAL: " + dinero(venta.getTotal()), 330, 52, 200, 25, 13));
        d.setSummary(resumen);

        JasperPrint jp = JasperFillManager.fillReport(
                JasperCompileManager.compileReport(d),
                new HashMap<>(),
                new JRBeanCollectionDataSource(filas)
        );
        JasperExportManager.exportReportToPdfFile(jp, destino.toAbsolutePath().toString());
        return destino;
    }

    /** JasperReports agrega valores al mapa durante el llenado: debe ser mutable (no Map.of). */
    private static Map<String, Object> parametros(String clave, Object valor) {
        Map<String, Object> p = new HashMap<>();
        p.put(clave, valor);
        return p;
    }

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static LocalDateTime inicio(LocalDate d) { return d.atStartOfDay(); }

    /** Límite superior exclusivo: incluye todo el día {@code hasta}. */
    private static LocalDateTime finExclusivo(LocalDate d) { return d.plusDays(1).atStartOfDay(); }

    private static String formatear(LocalDate d) { return d == null ? "" : d.format(FMT_FECHA); }

    private static String formatear(LocalDateTime d) { return d == null ? "" : d.format(FMT_FECHA_HORA); }

    private static String dinero(BigDecimal v) {
        return "C$ " + (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String nombreCliente(Cliente c) {
        return c == null ? "Consumidor final" : c.getNombres() + " " + c.getApellidos();
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new IllegalArgumentException("Rango de fechas inválido");
        }
    }

    private JasperDesign baseDesign(String nombre) {
        JasperDesign d = new JasperDesign();
        d.setName(nombre);
        d.setPageWidth(595);
        d.setPageHeight(842);
        d.setColumnWidth(535);
        d.setLeftMargin(30);
        d.setRightMargin(30);
        d.setTopMargin(30);
        d.setBottomMargin(30);
        return d;
    }

    private JasperDesign disenoPedidos() throws JRException {
        JasperDesign d = baseDesign("informe_pedidos");
        agregarTituloConSubtitulo(d, "BunBun Hub - Informe de Pedidos");
        agregarColumnas(d,
                new String[]{"ID", "Fecha", "Cliente", "Estado", "Total"},
                new String[]{"id", "fecha", "cliente", "estado", "total"},
                new int[]{0, 48, 135, 342, 445},
                new int[]{45, 84, 204, 100, 90});
        return d;
    }

    private JasperDesign disenoVentas() throws JRException {
        JasperDesign d = baseDesign("informe_ventas");
        agregarTituloConSubtitulo(d, "BunBun Hub - Informe de Ventas");
        agregarColumnas(d,
                new String[]{"Factura", "Fecha", "Cliente", "Subtotal", "Total"},
                new String[]{"factura", "fecha", "cliente", "subtotal", "total"},
                new int[]{0, 95, 180, 365, 450},
                new int[]{90, 80, 180, 80, 85});
        return d;
    }

    private void agregarTituloConSubtitulo(JasperDesign d, String tituloTexto) throws JRException {
        JRDesignParameter param = new JRDesignParameter();
        param.setName("SUBTITULO");
        param.setValueClass(String.class);
        d.addParameter(param);

        JRDesignBand titulo = new JRDesignBand();
        titulo.setHeight(68);
        titulo.addElement(textoFijo(tituloTexto, 0, 0, 535, 28, 18));
        titulo.addElement(textoCampo("$P{SUBTITULO}", 0, 34, 535, 22, 11));
        d.setTitle(titulo);
    }

    private void agregarColumnas(JasperDesign d, String[] nombres, String[] props, int[] x, int[] w) throws JRException {
        JRDesignBand cabecera = new JRDesignBand();
        cabecera.setHeight(25);
        JRDesignBand detalle = new JRDesignBand();
        detalle.setHeight(22);
        for (int i = 0; i < props.length; i++) {
            JRDesignField f = new JRDesignField();
            f.setName(props[i]);
            f.setValueClass(String.class);
            d.addField(f);
            cabecera.addElement(textoFijo(nombres[i], x[i], 0, w[i], 21, 10));
            detalle.addElement(textoCampo("$F{" + props[i] + "}", x[i], 0, w[i], 20, 9));
        }
        d.setColumnHeader(cabecera);
        ((JRDesignSection) d.getDetailSection()).addBand(detalle);
    }

    private JRDesignStaticText textoFijo(String s, int x, int y, int ancho, int alto, int fuente) {
        JRDesignStaticText e = new JRDesignStaticText();
        e.setText(s);
        e.setX(x);
        e.setY(y);
        e.setWidth(ancho);
        e.setHeight(alto);
        e.setFontSize((float) fuente);
        return e;
    }

    private JRDesignTextField textoCampo(String expr, int x, int y, int ancho, int alto, int fuente) {
        JRDesignTextField e = new JRDesignTextField();
        e.setExpression(new JRDesignExpression(expr));
        e.setX(x);
        e.setY(y);
        e.setWidth(ancho);
        e.setHeight(alto);
        e.setFontSize((float) fuente);
        return e;
    }

    public static final class FilaPedido {
        private final String id, fecha, cliente, estado, total;
        public FilaPedido(Long id, String fecha, String cliente, String estado, String total) {
            this.id = id.toString(); this.fecha = fecha; this.cliente = cliente;
            this.estado = estado; this.total = total;
        }
        public String getId() { return id; }
        public String getFecha() { return fecha; }
        public String getCliente() { return cliente; }
        public String getEstado() { return estado; }
        public String getTotal() { return total; }
    }

    public static final class FilaVenta {
        private final String factura, fecha, cliente, subtotal, total;
        public FilaVenta(String factura, String fecha, String cliente, String subtotal, String total) {
            this.factura = factura; this.fecha = fecha; this.cliente = cliente;
            this.subtotal = subtotal; this.total = total;
        }
        public String getFactura() { return factura; }
        public String getFecha() { return fecha; }
        public String getCliente() { return cliente; }
        public String getSubtotal() { return subtotal; }
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