package inventario.vista;

import static inventario.vista.Componentes.*;

import inventario.modelo.*;
import inventario.servicio.InventarioServicio;

import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.*;

import javax.swing.*;

// Selección, consulta y exportación de reportes.
public final class ReportesPanel extends PanelModulo {
    private final JComboBox<String> reporte = combo("reporte");
    private final JTextField filtroReporte = campo("filtroReporte", ""),
            fechaReporte = campo("fechaReporte", LocalDate.now().toString()),
            diasReporte = campo("diasReporte", "90");
    private final JButton consultarReporte = boton("consultarReporte", "Consultar", true),
            exportarReporte = boton("exportarReporte", "Exportar CSV", false),
            imprimirReporte = boton("imprimirReporte", "Imprimir", false);
    private final JTable tablaReporte = tabla("tablaReporte", "Resultado");
    private final JLabel mensajeReporte = aviso("mensajeReporte", "Selecciona un reporte.");
    private String tituloReporteGenerado = "";
    private boolean reporteGenerado;

    public ReportesPanel(InventarioServicio servicio, EjecutorTareas tareas) {
        super(servicio, tareas);
        add(pantallaReportes());
        actualizarControles();
    }

    @Override
    protected void actualizarControles() {
        boolean listo = !tareas.ocupado();
        consultarReporte.setEnabled(listo);
        reporte.setEnabled(listo);
        exportarReporte.setEnabled(listo && reporteGenerado);
        imprimirReporte.setEnabled(listo && reporteGenerado);
        actualizarParametrosReporte();
    }

    private JPanel pantallaReportes() {
        var panel =
                pantalla(
                        "Reportes del inventario",
                        "Consulta y exporta información sobre los equipos registrados.");
        String[] nombres = {
            "01 · Hoja de vida de equipos", "02 · Equipos por oficina",
                    "03 · Equipos pendientes de reparación",
            "04 · Puestos sin responsable", "05 · Identificación por completar",
                    "06 · Direcciones IP repetidas",
            "07 · Garantías próximas a vencer", "08 · Antigüedad por oficina",
                    "09 · Cobertura de software",
            "10 · Versiones de Windows y Office", "11 · Equipos por trabajador",
                    "12 · Periféricos por verificar",
            "13 · Memoria RAM inferior a 8 GB", "14 · Fechas por completar",
                    "15 · Uso del catálogo de software"
        };
        reporte.setModel(new DefaultComboBoxModel<>(nombres));
        var controles = formulario();
        fila(controles, 0, "Reporte", reporte);
        fila(controles, 1, "Buscar en resultados", filtroReporte);
        var parametros =
                botones(
                        texto("Fecha de corte", 13, true, TINTA),
                        fechaReporte,
                        texto("Plazo en días", 13, true, TINTA),
                        diasReporte);
        fechaReporte.setColumns(10);
        diasReporte.setColumns(5);
        fila(controles, 2, "", parametros);
        fila(controles, 3, "", botones(consultarReporte, exportarReporte, imprimirReporte));
        var cuerpo = new JPanel(new BorderLayout(0, 12));
        cuerpo.setOpaque(false);
        cuerpo.add(controles, BorderLayout.NORTH);
        tablaReporte.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        cuerpo.add(new JScrollPane(tablaReporte));
        cuerpo.add(mensajeReporte, BorderLayout.SOUTH);
        panel.add(cuerpo);
        reporte.addActionListener(
                e -> {
                    limpiarReporte();
                    actualizarParametrosReporte();
                });
        consultarReporte.addActionListener(e -> cargarReporte());
        filtroReporte.addActionListener(e -> cargarReporte());
        exportarReporte.addActionListener(e -> exportarReporte());
        imprimirReporte.addActionListener(
                e -> {
                    try {
                        tablaReporte.print(
                                JTable.PrintMode.FIT_WIDTH,
                                new java.text.MessageFormat(tituloReporteGenerado),
                                new java.text.MessageFormat("Página {0}"));
                    } catch (java.awt.print.PrinterException error) {
                        mensaje(mensajeReporte, "No se pudo imprimir el reporte.", ROJO);
                    }
                });
        actualizarParametrosReporte();
        return panel;
    }

    private void actualizarParametrosReporte() {
        int numero = reporte.getSelectedIndex() + 1;
        fechaReporte.setEnabled(!tareas.ocupado() && (numero == 7 || numero == 8));
        diasReporte.setEnabled(!tareas.ocupado() && numero == 7);
    }

    private void limpiarReporte() {
        modeloTabla(tablaReporte).setRowCount(0);
        reporteGenerado = false;
        mensaje(mensajeReporte, "Pulsa Consultar para obtener el reporte.", SUAVE);
        actualizarControles();
    }

    private void cargarReporte() {
        if (tareas.ocupado()) return;
        try {
            int numero = reporte.getSelectedIndex() + 1;
            LocalDate corte = (numero == 7 || numero == 8) ? fecha(fechaReporte) : LocalDate.now();
            int dias = numero == 7 ? Integer.parseInt(diasReporte.getText().strip()) : 90;
            if (corte == null || dias < 1 || dias > 3650)
                throw new IllegalArgumentException(
                        "Indica una fecha y un plazo entre 1 y 3650 días.");
            String filtro = filtroReporte.getText().strip();
            String titulo = reporte.getSelectedItem().toString();
            limpiarReporte();
            ejecutar(
                    () -> servicio.reporte(numero, filtro, corte, dias),
                    informe -> {
                        modeloTabla(tablaReporte)
                                .setColumnIdentifiers(
                                        informe.columnas().stream()
                                                .map(Exportar::titulo)
                                                .toArray());
                        for (var fila : informe.filas())
                            modeloTabla(tablaReporte).addRow(fila.toArray());
                        for (int i = 0; i < tablaReporte.getColumnCount(); i++)
                            tablaReporte.getColumnModel().getColumn(i).setPreferredWidth(185);
                        tituloReporteGenerado = titulo;
                        reporteGenerado = true;
                        mensaje(
                                mensajeReporte,
                                informe.filas().size()
                                        + " resultados. Puedes exportarlos o imprimirlos.",
                                SUAVE);
                    },
                    mensajeReporte);
        } catch (NumberFormatException error) {
            mensaje(mensajeReporte, "El plazo debe ser un número entero de días.", ROJO);
        } catch (IllegalArgumentException error) {
            mensaje(mensajeReporte, error.getMessage(), ROJO);
        }
    }

    private void exportarReporte() {
        var selector = new JFileChooser();
        selector.setSelectedFile(new java.io.File("Reporte_Inventario.csv"));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path archivo = selector.getSelectedFile().toPath();
        if (!archivo.toString().toLowerCase(Locale.ROOT).endsWith(".csv"))
            archivo = Path.of(archivo + ".csv");
        if (Files.exists(archivo) && !confirmar("¿Reemplazar el archivo existente?")) return;
        try {
            Exportar.csv(tablaReporte, archivo);
            mensaje(mensajeReporte, "Reporte guardado en " + archivo.getFileName() + ".", VERDE);
        } catch (IOException error) {
            mensaje(
                    mensajeReporte,
                    "No se pudo guardar. Cierra el archivo si está abierto y vuelve a intentar.",
                    ROJO);
        }
    }
}
