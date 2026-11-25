package ni.edu.uam.proyectofinalpae.report;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.*;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Diseño común de los reportes JasperReports de BunBun Hub, construido por código
 * (sin plantilla .jrxml) con la paleta de la aplicación:
 * encabezado azul marino, tarjeta de datos, tabla con filas alternas,
 * cuadro de totales y pie con numeración de páginas.
 */
final class DisenoReporte {

    // Paleta (igual que global.css)
    static final Color NAVY = new Color(0x2F, 0x41, 0x56);
    static final Color TEAL = new Color(0x56, 0x7C, 0x8D);
    static final Color SKY = new Color(0xC8, 0xD9, 0xE6);
    static final Color BEIGE = new Color(0xF5, 0xEF, 0xEB);
    static final Color LINEA = new Color(0xE3, 0xED, 0xF5);
    static final Color BLANCO = Color.WHITE;

    /** Fuente incluida en jasperreports-fonts: se ve igual en la vista previa y en el PDF. */
    private static final String FUENTE = "DejaVu Sans";

    static final int ANCHO_PAGINA = 595;
    static final int ALTO_PAGINA = 842;
    static final int MARGEN = 30;
    static final int ANCHO = ANCHO_PAGINA - 2 * MARGEN; // 535

    /** Columna de la tabla: el campo es una propiedad String del bean de la fila. */
    record Columna(String titulo, String campo, int ancho, HorizontalTextAlignEnum alineacion) {
        static Columna izq(String titulo, String campo, int ancho) {
            return new Columna(titulo, campo, ancho, HorizontalTextAlignEnum.LEFT);
        }
        static Columna der(String titulo, String campo, int ancho) {
            return new Columna(titulo, campo, ancho, HorizontalTextAlignEnum.RIGHT);
        }
        static Columna centro(String titulo, String campo, int ancho) {
            return new Columna(titulo, campo, ancho, HorizontalTextAlignEnum.CENTER);
        }
    }

    /** Par etiqueta/valor para la tarjeta de datos o el cuadro de totales. */
    record Dato(String etiqueta, String valor, boolean destacado) {
        static Dato de(String etiqueta, String valor) { return new Dato(etiqueta, valor, false); }
        static Dato total(String etiqueta, String valor) { return new Dato(etiqueta, valor, true); }
    }

    private DisenoReporte() {}

    /**
     * @param titulo    título grande del encabezado (p. ej. "Informe de Pedidos")
     * @param subtitulo línea bajo el título (p. ej. el rango de fechas); puede ser null
     * @param datos     tarjeta de datos (cliente, factura...); puede estar vacía
     * @param columnas  columnas de la tabla; la suma de anchos debe ser {@link #ANCHO}
     * @param totales   cuadro de totales al final; puede estar vacío
     * @param sinDatos  mensaje cuando no hay filas
     */
    static JasperDesign construir(String nombre, String titulo, String subtitulo, List<Dato> datos,
                                  List<Columna> columnas, List<Dato> totales, String sinDatos) throws JRException {
        int suma = columnas.stream().mapToInt(Columna::ancho).sum();
        if (suma != ANCHO) throw new IllegalArgumentException("Las columnas de " + nombre + " suman " + suma + ", no " + ANCHO);

        JasperDesign d = new JasperDesign();
        d.setName(nombre);
        d.setPageWidth(ANCHO_PAGINA);
        d.setPageHeight(ALTO_PAGINA);
        d.setColumnWidth(ANCHO);
        d.setLeftMargin(MARGEN);
        d.setRightMargin(MARGEN);
        d.setTopMargin(MARGEN);
        d.setBottomMargin(MARGEN);
        // Sin filas igual se imprimen encabezado, totales y el mensaje de "sin datos"
        d.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);

        for (Columna c : columnas) {
            JRDesignField f = new JRDesignField();
            f.setName(c.campo());
            f.setValueClass(String.class);
            d.addField(f);
        }

        d.setTitle(bandaTitulo(titulo, subtitulo, datos));
        d.setColumnHeader(bandaCabecera(columnas));
        ((JRDesignSection) d.getDetailSection()).addBand(bandaDetalle(columnas));
        d.setSummary(bandaResumen(totales, sinDatos));
        d.setPageFooter(bandaPie());
        return d;
    }

    // ------------------------------------------------------------------ bandas

    private static JRDesignBand bandaTitulo(String titulo, String subtitulo, List<Dato> datos) {
        JRDesignBand b = new JRDesignBand();
        int y = 0;

        // Franja superior azul marino
        b.addElement(rect(0, 0, ANCHO, 70, NAVY));
        b.addElement(rect(0, 70, ANCHO, 4, TEAL));
        b.addElement(texto("BunBun Hub", 18, 12, 300, 26, 20, BLANCO, true, HorizontalTextAlignEnum.LEFT));
        b.addElement(texto(titulo, 18, 40, 330, 20, 12, SKY, false, HorizontalTextAlignEnum.LEFT));
        String generado = "Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        b.addElement(texto(generado, ANCHO - 218, 14, 200, 14, 8, SKY, false, HorizontalTextAlignEnum.RIGHT));
        y = 84;

        if (subtitulo != null && !subtitulo.isBlank()) {
            b.addElement(texto(subtitulo, 0, y, ANCHO, 18, 11, NAVY, true, HorizontalTextAlignEnum.LEFT));
            y += 24;
        }

        if (datos != null && !datos.isEmpty()) {
            // Tarjeta beige con los datos en dos columnas
            int filas = (datos.size() + 1) / 2;
            int alto = 14 + filas * 18;
            b.addElement(rect(0, y, ANCHO, alto, BEIGE));
            b.addElement(rect(0, y, 4, alto, TEAL));
            for (int i = 0; i < datos.size(); i++) {
                Dato dato = datos.get(i);
                int col = i % 2, fila = i / 2;
                int x = 16 + col * (ANCHO / 2);
                int yy = y + 8 + fila * 18;
                b.addElement(texto(dato.etiqueta() + ":", x, yy, 90, 16, 9, TEAL, true, HorizontalTextAlignEnum.LEFT));
                b.addElement(texto(dato.valor(), x + 92, yy, ANCHO / 2 - 110, 16, 9, NAVY, dato.destacado(), HorizontalTextAlignEnum.LEFT));
            }
            y += alto + 10;
        }

        b.setHeight(y + 4);
        return b;
    }

    private static JRDesignBand bandaCabecera(List<Columna> columnas) {
        JRDesignBand b = new JRDesignBand();
        b.setHeight(24);
        b.addElement(rect(0, 0, ANCHO, 24, NAVY));
        int x = 0;
        for (Columna c : columnas) {
            JRDesignStaticText t = texto(c.titulo(), x, 0, c.ancho(), 24, 9, BLANCO, true, c.alineacion());
            padding(t);
            b.addElement(t);
            x += c.ancho();
        }
        return b;
    }

    private static JRDesignBand bandaDetalle(List<Columna> columnas) {
        JRDesignBand b = new JRDesignBand();
        b.setHeight(20);

        // Filas alternas en beige, estiradas a la altura real de la fila
        JRDesignRectangle zebra = rect(0, 0, ANCHO, 20, BEIGE);
        zebra.setStretchType(StretchTypeEnum.CONTAINER_HEIGHT);
        zebra.setPrintWhenExpression(new JRDesignExpression("$V{REPORT_COUNT} % 2 == 0"));
        b.addElement(zebra);

        int x = 0;
        for (Columna c : columnas) {
            JRDesignTextField t = new JRDesignTextField();
            t.setExpression(new JRDesignExpression("$F{" + c.campo() + "}"));
            t.setX(x);
            t.setY(0);
            t.setWidth(c.ancho());
            t.setHeight(20);
            t.setBlankWhenNull(true);
            t.setTextAdjust(TextAdjustEnum.STRETCH_HEIGHT); // textos largos bajan de línea
            t.setStretchType(StretchTypeEnum.CONTAINER_HEIGHT);
            fuente(t, 9, NAVY, false, c.alineacion());
            padding(t);
            b.addElement(t);
            x += c.ancho();
        }

        JRDesignLine linea = new JRDesignLine();
        linea.setX(0);
        linea.setY(19);
        linea.setWidth(ANCHO);
        linea.setHeight(0);
        linea.setPositionType(PositionTypeEnum.FLOAT);
        linea.setForecolor(LINEA);
        b.addElement(linea);
        return b;
    }

    private static JRDesignBand bandaResumen(List<Dato> totales, String sinDatos) {
        JRDesignBand b = new JRDesignBand();
        int y = 0;

        // Mensaje solo cuando no hubo filas
        JRDesignStaticText vacio = texto(sinDatos, 0, 6, ANCHO, 30, 10, TEAL, false, HorizontalTextAlignEnum.CENTER);
        vacio.setPrintWhenExpression(new JRDesignExpression("$V{REPORT_COUNT} == 0"));
        b.addElement(vacio);
        y = 40;

        if (totales != null && !totales.isEmpty()) {
            int anchoCaja = 250, x = ANCHO - anchoCaja;
            for (Dato t : totales) {
                int alto = t.destacado() ? 26 : 20;
                if (t.destacado()) {
                    b.addElement(rect(x, y, anchoCaja, alto, NAVY));
                    b.addElement(texto(t.etiqueta(), x + 10, y, 130, alto, 11, BLANCO, true, HorizontalTextAlignEnum.LEFT));
                    b.addElement(texto(t.valor(), x + 120, y, anchoCaja - 130, alto, 11, BLANCO, true, HorizontalTextAlignEnum.RIGHT));
                } else {
                    b.addElement(rect(x, y, anchoCaja, alto, BEIGE));
                    b.addElement(texto(t.etiqueta(), x + 10, y, 150, alto, 9, TEAL, true, HorizontalTextAlignEnum.LEFT));
                    b.addElement(texto(t.valor(), x + 120, y, anchoCaja - 130, alto, 9, NAVY, true, HorizontalTextAlignEnum.RIGHT));
                }
                y += alto + 2;
            }
        }
        b.setHeight(y + 6);
        return b;
    }

    private static JRDesignBand bandaPie() {
        JRDesignBand b = new JRDesignBand();
        b.setHeight(26);
        JRDesignLine linea = new JRDesignLine();
        linea.setX(0);
        linea.setY(4);
        linea.setWidth(ANCHO);
        linea.setHeight(0);
        linea.setForecolor(SKY);
        b.addElement(linea);
        b.addElement(texto("BunBun Hub · Reporte generado automáticamente", 0, 8, 300, 14, 8, TEAL, false, HorizontalTextAlignEnum.LEFT));

        JRDesignTextField pagina = new JRDesignTextField();
        pagina.setExpression(new JRDesignExpression("\"Página \" + $V{PAGE_NUMBER} + \" de\""));
        pagina.setX(ANCHO - 140);
        pagina.setY(8);
        pagina.setWidth(110);
        pagina.setHeight(14);
        fuente(pagina, 8, TEAL, false, HorizontalTextAlignEnum.RIGHT);
        b.addElement(pagina);

        JRDesignTextField total = new JRDesignTextField();
        total.setExpression(new JRDesignExpression("\" \" + $V{PAGE_NUMBER}"));
        total.setEvaluationTime(EvaluationTimeEnum.REPORT); // total de páginas
        total.setX(ANCHO - 30);
        total.setY(8);
        total.setWidth(30);
        total.setHeight(14);
        fuente(total, 8, TEAL, false, HorizontalTextAlignEnum.LEFT);
        b.addElement(total);
        return b;
    }

    // --------------------------------------------------------------- elementos

    private static JRDesignRectangle rect(int x, int y, int w, int h, Color color) {
        JRDesignRectangle r = new JRDesignRectangle();
        r.setX(x);
        r.setY(y);
        r.setWidth(w);
        r.setHeight(h);
        r.setMode(ModeEnum.OPAQUE);
        r.setBackcolor(color);
        r.setForecolor(color);
        r.getLinePen().setLineWidth(0f);
        return r;
    }

    private static JRDesignStaticText texto(String s, int x, int y, int w, int h, float tam, Color color,
                                            boolean negrita, HorizontalTextAlignEnum alineacion) {
        JRDesignStaticText t = new JRDesignStaticText();
        t.setText(s == null ? "" : s);
        t.setX(x);
        t.setY(y);
        t.setWidth(w);
        t.setHeight(h);
        fuente(t, tam, color, negrita, alineacion);
        return t;
    }

    private static void fuente(JRDesignTextElement t, float tam, Color color, boolean negrita,
                               HorizontalTextAlignEnum alineacion) {
        t.setFontName(FUENTE);
        t.setFontSize(tam);
        t.setForecolor(color);
        t.setBold(negrita);
        t.setHorizontalTextAlign(alineacion);
        t.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
    }

    private static void padding(JRDesignTextElement t) {
        t.getLineBox().setLeftPadding(6);
        t.getLineBox().setRightPadding(6);
    }
}
