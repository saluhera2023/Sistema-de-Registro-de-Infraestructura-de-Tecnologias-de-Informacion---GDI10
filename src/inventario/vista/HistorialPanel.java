package inventario.vista;

import static inventario.vista.Componentes.*;

import inventario.modelo.*;
import inventario.servicio.InventarioServicio;

import java.awt.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

// Presenta la auditoría sin intervenir en las operaciones de los otros módulos.
public final class HistorialPanel extends PanelModulo {
    private final JTextField filtroAuditoria = campo("filtroAuditoria", "");
    private final JButton buscarAuditoria = boton("buscarAuditoria", "Consultar historial", true);
    private final JTable tablaAuditoria =
            tabla(
                    "tablaAuditoria",
                    "ID",
                    "Fecha",
                    "Usuario",
                    "Registro",
                    "Acción",
                    "Identificador");
    private final JTextArea antes = area("antes"), despues = area("despues");
    private List<Auditoria> filasAuditoria = List.of();
    private final JLabel mensajeAuditoria =
            aviso("mensajeAuditoria", "Se muestran hasta 200 cambios recientes.");

    public HistorialPanel(InventarioServicio servicio, EjecutorTareas tareas) {
        super(servicio, tareas);
        add(pantallaAuditoria());
        buscarAuditoria.addActionListener(e -> cargar());
        filtroAuditoria.addActionListener(e -> cargar());
        tablaAuditoria
                .getSelectionModel()
                .addListSelectionListener(
                        e -> {
                            int fila = tablaAuditoria.getSelectedRow();
                            if (fila < 0) return;
                            var valor =
                                    filasAuditoria.get(tablaAuditoria.convertRowIndexToModel(fila));
                            antes.setText(formatear(valor.anterior()));
                            despues.setText(formatear(valor.nuevo()));
                            antes.setCaretPosition(0);
                            despues.setCaretPosition(0);
                        });
    }

    @Override
    public void cargar() {
        cargarAuditoria();
    }

    @Override
    protected void actualizarControles() {
        buscarAuditoria.setEnabled(!tareas.ocupado());
    }

    private JPanel pantallaAuditoria() {
        var panel =
                pantalla(
                        "Historial de cambios",
                        "Consulta los cambios realizados y compara sus valores.");
        var contenido = new JPanel(new BorderLayout(0, 10));
        contenido.setOpaque(false);
        var busqueda = new JPanel(new BorderLayout(10, 0));
        busqueda.setOpaque(false);
        busqueda.add(filtroAuditoria);
        busqueda.add(buscarAuditoria, BorderLayout.EAST);
        contenido.add(busqueda, BorderLayout.NORTH);
        var division = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        division.setResizeWeight(0.59);
        division.setTopComponent(new JScrollPane(tablaAuditoria));
        var valores = new JPanel(new GridLayout(1, 2, 10, 0));
        var previo = new JPanel(new BorderLayout());
        previo.add(texto("Valores anteriores", 13, true, TINTA), BorderLayout.NORTH);
        previo.add(new JScrollPane(antes));
        var posterior = new JPanel(new BorderLayout());
        posterior.add(texto("Valores nuevos", 13, true, TINTA), BorderLayout.NORTH);
        posterior.add(new JScrollPane(despues));
        valores.add(previo);
        valores.add(posterior);
        division.setBottomComponent(valores);
        contenido.add(division);
        contenido.add(mensajeAuditoria, BorderLayout.SOUTH);
        panel.add(contenido);
        return panel;
    }

    private void cargarAuditoria() {
        String termino = filtroAuditoria.getText().strip();
        antes.setText("");
        despues.setText("");
        ejecutar(
                () -> servicio.auditoria(termino),
                filas -> {
                    filasAuditoria = filas;
                    modeloTabla(tablaAuditoria).setRowCount(0);
                    for (var fila : filas)
                        modeloTabla(tablaAuditoria)
                                .addRow(
                                        new Object[] {
                                            fila.id(),
                                            fila.fecha(),
                                            fila.usuario(),
                                            fila.tabla(),
                                            fila.operacion(),
                                            fila.clave()
                                        });
                    mensaje(
                            mensajeAuditoria,
                            filas.size()
                                    + " cambios recuperados. Selecciona uno para ver sus valores.",
                            SUAVE);
                },
                mensajeAuditoria);
    }
}
