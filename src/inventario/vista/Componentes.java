package inventario.vista;

import inventario.modelo.*;

import java.awt.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

// Estilo y controles comunes. No guarda selecciones ni datos de los módulos.
final class Componentes {
    private Componentes() {}

    static final Color FONDO = new Color(0xEFF3F7),
            TINTA = new Color(0x172B3C),
            SUAVE = new Color(0x4F6576),
            AZUL = new Color(0x235981),
            VERDE = new Color(0x17634B),
            ROJO = new Color(0xA53532);

    static String primeraLinea(String texto) {
        return texto.split("\\R", 2)[0].replaceFirst("^ERROR: ", "");
    }

    static String formatear(String json) {
        return json == null ? "Sin valores" : json.replace(", \"", ",\n\"");
    }

    static String vacio(String valor) {
        return valor == null ? "" : valor;
    }

    static String valor(String valor) {
        return valor == null || valor.isBlank() ? "Sin consignar" : valor;
    }

    static String esc(String valor) {
        return valor(valor).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static String fecha(LocalDate valor) {
        return valor == null ? "" : valor.toString();
    }

    static LocalDate fecha(JTextField campo) {
        if (campo.getText().isBlank()) return null;
        try {
            return LocalDate.parse(campo.getText().strip());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Usa una fecha válida en formato AAAA-MM-DD.");
        }
    }

    static void exigir(JTextField campo, String nombre, int limite) {
        if (campo.getText().isBlank())
            throw new IllegalArgumentException(nombre + " es obligatorio.");
        limitar(campo, limite);
    }

    static void limitar(JTextField campo, int limite) {
        String texto = campo.getText().strip();
        if (texto.codePointCount(0, texto.length()) > limite) {
            throw new IllegalArgumentException(
                    "El campo " + campo.getName() + " admite hasta " + limite + " caracteres.");
        }
    }

    static Integer id(JComboBox<Catalogo> combo) {
        var valor = (Catalogo) combo.getSelectedItem();
        return valor == null ? null : valor.id();
    }

    static void llenar(
            JComboBox<Catalogo> caja,
            List<Catalogo> datos,
            String grupo,
            String inicial,
            Integer excluir) {
        var opciones = new DefaultComboBoxModel<Catalogo>();
        opciones.addElement(new Catalogo(grupo, null, inicial));
        for (var fila : datos)
            if (fila.grupo().equals(grupo) && !Objects.equals(fila.id(), excluir))
                opciones.addElement(fila);
        caja.setModel(opciones);
    }

    static void seleccionar(JComboBox<Catalogo> caja, Integer id) {
        for (int i = 0; i < caja.getItemCount(); i++) {
            if (Objects.equals(caja.getItemAt(i).id(), id)) {
                caja.setSelectedIndex(i);
                return;
            }
        }
        if (id != null) {
            // Una referencia histórica se conserva hasta que el usuario elija otra.
            caja.addItem(
                    new Catalogo(
                            "historico", id, "Referencia " + id + " sin disponibilidad actual"));
            caja.setSelectedIndex(caja.getItemCount() - 1);
        }
    }

    static void llenarRegistros(JComboBox<Registro> caja, List<Registro> datos) {
        caja.setModel(new DefaultComboBoxModel<>(datos.toArray(Registro[]::new)));
    }

    static String htmlFicha(Ficha ficha) {
        var html =
                new StringBuilder(
                        "<html><body style='font-family:sans-serif;font-size:12pt;color:#172b3c'>");
        html.append("<h2>")
                .append(esc(ficha.codigo()))
                .append(" · ")
                .append(esc(ficha.nombre()))
                .append("</h2>");
        html.append("<table width='100%' cellpadding='5' cellspacing='0'>");
        int i = 0;
        for (var dato : ficha.datos().entrySet()) {
            if (i % 2 == 0) html.append("<tr bgcolor='#eff3f7'>");
            html.append("<td width='50%'><b>")
                    .append(dato.getKey())
                    .append("</b> &nbsp;")
                    .append(esc(dato.getValue()))
                    .append("</td>");
            if (i++ % 2 == 1) html.append("</tr>");
        }
        html.append("</table><br><table width='100%' cellpadding='7'><tr>");
        seccionFicha(html, "Periféricos", ficha.perifericos());
        seccionFicha(html, "Componentes", ficha.componentes());
        seccionFicha(html, "Software", ficha.software());
        return html.append("</tr></table></body></html>").toString();
    }

    static void seccionFicha(StringBuilder html, String titulo, List<String> filas) {
        html.append("<td width='33%' valign='top'><b>").append(titulo).append("</b><br>");
        for (String fila : filas) html.append("&#8226; ").append(esc(fila)).append("<br>");
        if (filas.isEmpty()) html.append("Sin registros.");
        html.append("</td>");
    }

    static JPanel pantalla(String titulo, String subtitulo) {
        var panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 22, 18, 22));
        var encabezado = columna();
        encabezado.add(texto(titulo, 22, true, TINTA));
        encabezado.add(Box.createVerticalStrut(5));
        encabezado.add(texto(subtitulo, 14, false, SUAVE));
        panel.add(encabezado, BorderLayout.NORTH);
        return panel;
    }

    static JPanel columna() {
        var panel = new JPanel();
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    static JPanel botones(Component... controles) {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 2));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (var control : controles) panel.add(control);
        return panel;
    }

    static JPanel dosColumnas(JPanel izquierda, JPanel derecha) {
        var panel = new JPanel(new GridLayout(1, 2, 24, 0));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (var contenido : List.of(izquierda, derecha)) {
            var bloque = new JPanel(new BorderLayout());
            bloque.setOpaque(false);
            bloque.add(contenido, BorderLayout.NORTH);
            panel.add(bloque);
        }
        return panel;
    }

    static JPanel formulario() {
        var panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    static void fila(JPanel formulario, int fila, String etiqueta, JComponent control) {
        var reglas = new GridBagConstraints();
        reglas.gridy = fila;
        reglas.anchor = GridBagConstraints.LINE_START;
        reglas.insets = new Insets(6, 0, 6, 10);
        var titulo = texto(etiqueta, 14, true, TINTA);
        titulo.setLabelFor(control);
        formulario.add(titulo, reglas);
        reglas.gridx = 1;
        reglas.weightx = 1;
        reglas.fill = GridBagConstraints.HORIZONTAL;
        reglas.insets = new Insets(6, 0, 6, 0);
        control.setFont(new Font("Dialog", Font.PLAIN, 14));
        formulario.add(control, reglas);
    }

    static JLabel texto(String texto, int tamano, boolean negrita, Color color) {
        var etiqueta = new JLabel(texto);
        etiqueta.setAlignmentX(Component.LEFT_ALIGNMENT);
        etiqueta.setFont(new Font("Dialog", negrita ? Font.BOLD : Font.PLAIN, tamano));
        etiqueta.setForeground(color);
        return etiqueta;
    }

    static JLabel aviso(String id, String texto) {
        var etiqueta = texto(texto, 14, true, SUAVE);
        etiqueta.setName(id);
        return etiqueta;
    }

    static JTextField campo(String id, String valor) {
        var campo = new JTextField(valor, 18);
        campo.setName(id);
        campo.setFont(new Font("Dialog", Font.PLAIN, 14));
        return campo;
    }

    static <T> JComboBox<T> combo(String id) {
        var caja = new JComboBox<T>();
        caja.setName(id);
        caja.setPrototypeDisplayValue(null);
        return caja;
    }

    static JTextArea area(String id) {
        var texto = new JTextArea();
        texto.setName(id);
        texto.setEditable(false);
        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        texto.setFont(new Font("Monospaced", Font.PLAIN, 13));
        texto.setForeground(TINTA);
        texto.setAlignmentX(Component.LEFT_ALIGNMENT);
        return texto;
    }

    static JButton boton(String id, String texto, boolean principal) {
        var boton = new JButton(texto);
        boton.setName(id);
        boton.setFont(new Font("Dialog", Font.BOLD, 13));
        boton.setMargin(new Insets(7, 10, 7, 10));
        boton.setBackground(principal ? AZUL : FONDO);
        boton.setForeground(principal ? Color.WHITE : TINTA);
        return boton;
    }

    static JTable tabla(String id, String... columnas) {
        var tabla =
                new JTable(
                        new DefaultTableModel(columnas, 0) {
                            @Override
                            public boolean isCellEditable(int fila, int columna) {
                                return false;
                            }
                        }) {
                    @Override
                    public String getToolTipText(java.awt.event.MouseEvent evento) {
                        int fila = rowAtPoint(evento.getPoint()),
                                columna = columnAtPoint(evento.getPoint());
                        return fila < 0 || columna < 0
                                ? null
                                : Objects.toString(getValueAt(fila, columna), "Sin consignar");
                    }
                };
        tabla.setName(id);
        tabla.setFont(new Font("Dialog", Font.PLAIN, 13));
        tabla.getTableHeader().setFont(new Font("Dialog", Font.BOLD, 13));
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setRowHeight(28);
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(new Color(0xDFE7ED));
        tabla.setSelectionBackground(new Color(0xD7E7F4));
        tabla.setSelectionForeground(TINTA);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(true);
        return tabla;
    }

    static DefaultTableModel modeloTabla(JTable tabla) {
        return (DefaultTableModel) tabla.getModel();
    }

    static void mensaje(JLabel etiqueta, String texto, Color color) {
        etiqueta.setText(texto);
        etiqueta.setForeground(color);
    }
}
