package ni.edu.uam.proyectofinalpae.report;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import ni.edu.uam.proyectofinalpae.model.Pedido;
import ni.edu.uam.proyectofinalpae.model.Venta;
import ni.edu.uam.proyectofinalpae.model.DetalleVenta;
import ni.edu.uam.proyectofinalpae.util.Tx;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** JasperReports: consulta Hibernate y exporta PDF real, sin plantilla JRXML externa. */
public class ReporteService {
    public Path exportarPedidos(LocalDate desde, LocalDate hasta, Path destino) throws JRException {
        if (desde == null || hasta == null || hasta.isBefore(desde))
            throw new IllegalArgumentException("Rango de fechas inválido");
        List<FilaPedido> filas = Tx.read(s -> {
            List<Pedido> pedidos = s.createQuery(
                            "SELECT p FROM Pedido p JOIN FETCH p.cliente " +
                                    "WHERE p.fechaPedido BETWEEN :desde AND :hasta ORDER BY p.fechaPedido", Pedido.class)
                    .setParameter("desde", desde).setParameter("hasta", hasta).list();
            List<FilaPedido> lista = new ArrayList<>();
            for (Pedido p : pedidos) lista.add(new FilaPedido(p.getId(), p.getFechaPedido().toString(),
                    p.getCliente().getNombres() + " " + p.getCliente().getApellidos(),
                    p.getEstado().name(), p.getPrecioTotal().toPlainString()));
            return lista;
        });
        JasperReport reporte = JasperCompileManager.compileReport(diseno());
        JasperPrint relleno = JasperFillManager.fillReport(reporte,
                Map.of("SUBTITULO", "Pedidos: " + desde + " al " + hasta),
                new JRBeanCollectionDataSource(filas));
        JasperExportManager.exportReportToPdfFile(relleno, destino.toAbsolutePath().toString());
        return destino;
    }

    /** Exporta una factura con el detalle persistido de la venta. */
    public Path generarFactura(Venta venta, Path destino) throws JRException {
        if (venta == null || venta.getId() == null) throw new IllegalArgumentException("Venta no válida");
        List<FilaFactura> filas = new ArrayList<>();
        for (DetalleVenta item : venta.getDetalles()) {
            filas.add(new FilaFactura(item.getProducto().getNombre(),
                    String.valueOf(item.getCantidad()),
                    item.getPrecioUnitario().toPlainString(),
                    item.getSubtotal().toPlainString()));
        }
        JasperDesign d = new JasperDesign();
        d.setName("factura_venta");
        d.setPageWidth(595); d.setPageHeight(842);
        d.setColumnWidth(535); d.setLeftMargin(30); d.setRightMargin(30);
        d.setTopMargin(30); d.setBottomMargin(30);
        JRDesignBand titulo = new JRDesignBand(); titulo.setHeight(115);
        titulo.addElement(textoFijo("BunBun Hub - Factura", 0, 0, 535, 28, 18));
        titulo.addElement(textoFijo("Factura: " + venta.getNumeroFactura(), 0, 32, 535, 18, 11));
        titulo.addElement(textoFijo("Fecha: " + venta.getFecha(), 0, 53, 535, 18, 10));
        String cliente = venta.getCliente() == null ? "Consumidor final" : venta.getCliente().getNombres() + " " + venta.getCliente().getApellidos();
        titulo.addElement(textoFijo("Cliente: " + cliente, 0, 75, 535, 18, 10));
        d.setTitle(titulo);
        String[] nombres = {"Producto", "Cantidad", "Precio unitario", "Subtotal"};
        String[] props = {"producto", "cantidad", "precioUnitario", "subtotal"};
        int[] x = {0, 265, 340, 440};
        int[] w = {260, 70, 95, 90};
        JRDesignBand cabecera = new JRDesignBand(); cabecera.setHeight(25);
        JRDesignBand detalle = new JRDesignBand(); detalle.setHeight(22);
        for (int i = 0; i < props.length; i++) {
            JRDesignField campo = new JRDesignField();
            campo.setName(props[i]); campo.setValueClass(String.class); d.addField(campo);
            cabecera.addElement(textoFijo(nombres[i], x[i], 0, w[i], 20, 10));
            detalle.addElement(textoCampo("$F{" + props[i] + "}", x[i], 0, w[i], 20, 9));
        }
        d.setColumnHeader(cabecera);
        ((JRDesignSection)d.getDetailSection()).addBand(detalle);
        JRDesignBand resumen = new JRDesignBand(); resumen.setHeight(85);
        resumen.addElement(textoFijo("Subtotal: " + venta.getSubtotal(), 330, 5, 200, 20, 11));
        resumen.addElement(textoFijo("IVA (15%): " + venta.getImpuesto(), 330, 28, 200, 20, 11));
        resumen.addElement(textoFijo("TOTAL: " + venta.getTotal(), 330, 52, 200, 25, 13));
        d.setSummary(resumen);
        JasperPrint jp = JasperFillManager.fillReport(JasperCompileManager.compileReport(d),
                Map.of(), new JRBeanCollectionDataSource(filas));
        JasperExportManager.exportReportToPdfFile(jp, destino.toAbsolutePath().toString());
        return destino;
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

    private JasperDesign diseno() throws JRException {
        JasperDesign d = new JasperDesign();
        d.setName("informe_pedidos");
        d.setPageWidth(595); d.setPageHeight(842);
        d.setColumnWidth(535); d.setLeftMargin(30); d.setRightMargin(30);
        d.setTopMargin(30); d.setBottomMargin(30);
        JRDesignParameter param = new JRDesignParameter();
        param.setName("SUBTITULO"); param.setValueClass(String.class); d.addParameter(param);
        JRDesignBand titulo = new JRDesignBand(); titulo.setHeight(68);
        titulo.addElement(textoFijo("BunBun Hub - Informe de pedidos", 0, 0, 535, 28, 18));
        titulo.addElement(textoCampo("$P{SUBTITULO}", 0, 34, 535, 22, 11));
        d.setTitle(titulo);

        String[] nombres={"ID","Fecha","Cliente","Estado","Total"};
        String[] props={"id","fecha","cliente","estado","total"};
        int[] x={0,48,135,342,445};
        int[] w={45,84,204,100,90};
        JRDesignBand cabecera=new JRDesignBand();cabecera.setHeight(25);
        JRDesignBand detalle=new JRDesignBand();detalle.setHeight(22);
        for(int i=0;i<props.length;i++) {
            JRDesignField f=new JRDesignField();f.setName(props[i]);f.setValueClass(String.class);d.addField(f);
            cabecera.addElement(textoFijo(nombres[i],x[i],0,w[i],21,10));
            detalle.addElement(textoCampo("$F{"+props[i]+"}",x[i],0,w[i],20,9));
        }
        d.setColumnHeader(cabecera);
        ((JRDesignSection)d.getDetailSection()).addBand(detalle);
        return d;
    }
    private JRDesignStaticText textoFijo(String s,int x,int y,int ancho,int alto,int fuente) {
        JRDesignStaticText e=new JRDesignStaticText();e.setText(s);
        e.setX(x);e.setY(y);e.setWidth(ancho);e.setHeight(alto);
        e.setFontSize((float)fuente);return e;
    }
    private JRDesignTextField textoCampo(String expr,int x,int y,int ancho,int alto,int fuente) {
        JRDesignTextField e=new JRDesignTextField();
        e.setExpression(new JRDesignExpression(expr));
        e.setX(x);e.setY(y);e.setWidth(ancho);e.setHeight(alto);
        e.setFontSize((float)fuente);return e;
    }
    public static final class FilaPedido {
        private final String id,fecha,cliente,estado,total;
        public FilaPedido(Long id,String fecha,String cliente,String estado,String total) {
            this.id=id.toString();this.fecha=fecha;this.cliente=cliente;this.estado=estado;this.total=total;
        }
        public String getId(){return id;} public String getFecha(){return fecha;}
        public String getCliente(){return cliente;} public String getEstado(){return estado;}
        public String getTotal(){return total;}
    }
}
