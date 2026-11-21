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

// Conserva la selección y los formularios de software y componentes de un equipo.
public final class DetallesPanel extends PanelModulo {
    private Equipo equipoDetalles;
    private final JLabel tituloDetalles =
            texto("Software y componentes del equipo", 22, true, TINTA);
    private final JTable tablaDetalles =
            tabla("tablaDetalles", "Clase", "Nombre", "Fecha o detalle");
    private List<Detalle> filasDetalles = List.of();
    private final JComboBox<Registro> opcionSoftware = combo("opcionSoftware"),
            opcionComponente = combo("opcionComponente");
    private final JTextField
            fechaInstalacion = campo("fechaInstalacion", LocalDate.now().toString()),
            detalleComponente = campo("detalleComponente", "");
    private final JButton agregarSoftware = boton("agregarSoftware", "Agregar software", true),
            agregarComponente = boton("agregarComponente", "Agregar componente", true),
            cambiarFecha = boton("cambiarFecha", "Modificar detalle", false),
            quitarDetalle = boton("quitarDetalle", "Quitar asociación", false);
    private final JLabel mensajeDetalle =
            aviso("mensajeDetalle", "Selecciona un equipo en Inventario.");

    public DetallesPanel(InventarioServicio servicio, EjecutorTareas tareas) {
        super(servicio, tareas);
        add(pantallaDetalles());
        tablaDetalles
                .getSelectionModel()
                .addListSelectionListener(
                        e -> {
                            Detalle seleccionado = detalleElegido();
                            if (seleccionado != null) {
                                if (seleccionado.entidad().equals("software"))
                                    fechaInstalacion.setText(vacio(seleccionado.detalle()));
                                else detalleComponente.setText(vacio(seleccionado.detalle()));
                            }
                            actualizarControles();
                        });
        agregarSoftware.addActionListener(e -> agregar("software"));
        agregarComponente.addActionListener(e -> agregar("componente"));
        cambiarFecha.addActionListener(e -> cambiarDetalle(false));
        quitarDetalle.addActionListener(e -> cambiarDetalle(true));
        actualizarControles();
    }

    public void seleccionar(Equipo equipo) {
        equipoDetalles = equipo;
    }

    @Override
    public void cargar() {
        cargarDetalles();
    }

    @Override
    protected void actualizarControles() {
        boolean listo = !tareas.ocupado() && equipoDetalles != null;
        Detalle detalle = detalleElegido();
        agregarSoftware.setEnabled(listo && opcionSoftware.getItemCount() > 0);
        agregarComponente.setEnabled(listo && opcionComponente.getItemCount() > 0);
        cambiarFecha.setEnabled(listo && detalle != null);
        quitarDetalle.setEnabled(listo && detalle != null);
    }

    private JPanel pantallaDetalles() {
        var panel =
                pantalla(
                        "Detalles del equipo",
                        "Para elegir el equipo, usa Software y componentes desde Inventario.");
        var formularios = new JPanel(new GridLayout(1, 2, 20, 0));
        formularios.setOpaque(false);
        formularios.setAlignmentX(Component.LEFT_ALIGNMENT);
        var programa = formulario();
        fila(programa, 0, "Software", opcionSoftware);
        fila(programa, 1, "Fecha de instalación", fechaInstalacion);
        fila(programa, 2, "", agregarSoftware);
        var componente = formulario();
        fila(componente, 0, "Componente", opcionComponente);
        fila(componente, 1, "Especificación", detalleComponente);
        fila(componente, 2, "", agregarComponente);
        formularios.add(programa);
        formularios.add(componente);
        var superior = columna();
        superior.add(tituloDetalles);
        superior.add(Box.createVerticalStrut(10));
        superior.add(formularios);
        var contenido = new JPanel(new BorderLayout(0, 12));
        contenido.setOpaque(false);
        contenido.add(superior, BorderLayout.NORTH);
        contenido.add(new JScrollPane(tablaDetalles));
        var pie = columna();
        pie.add(botones(cambiarFecha, quitarDetalle));
        pie.add(Box.createVerticalStrut(8));
        pie.add(mensajeDetalle);
        contenido.add(pie, BorderLayout.SOUTH);
        panel.add(contenido);
        return panel;
    }

    private void cargarDetalles() {
        if (equipoDetalles == null) return;
        int equipo = equipoDetalles.id();
        ejecutar(
                () ->
                        new DatosDetalles(
                                servicio.registros("software", false),
                                servicio.registros("componente", false),
                                servicio.detalles(equipo)),
                datos -> {
                    tituloDetalles.setText("Detalles de " + equipoDetalles.codigo());
                    llenarRegistros(opcionSoftware, datos.software());
                    llenarRegistros(opcionComponente, datos.componentes());
                    filasDetalles = datos.detalles();
                    modeloTabla(tablaDetalles).setRowCount(0);
                    for (var fila : filasDetalles)
                        modeloTabla(tablaDetalles)
                                .addRow(
                                        new Object[] {
                                            fila.entidad().equals("software")
                                                    ? "Software"
                                                    : "Componente",
                                            fila.nombre(),
                                            valor(fila.detalle())
                                        });
                    mensaje(
                            mensajeDetalle,
                            filasDetalles.size() + " asociaciones registradas.",
                            SUAVE);
                },
                mensajeDetalle);
    }

    private void agregar(String entidad) {
        if (equipoDetalles == null || tareas.ocupado()) return;
        var seleccionado =
                (Registro)
                        (entidad.equals("software") ? opcionSoftware : opcionComponente)
                                .getSelectedItem();
        if (seleccionado == null) return;
        try {
            LocalDate fecha = entidad.equals("software") ? fecha(fechaInstalacion) : null;
            limitar(detalleComponente, 200);
            String especificacion = detalleComponente.getText().strip();
            ejecutar(
                    () -> {
                        servicio.vincular(
                                equipoDetalles.id(),
                                entidad,
                                seleccionado.clave(),
                                fecha,
                                "agregar",
                                null,
                                especificacion,
                                null);
                        return null;
                    },
                    nada -> SwingUtilities.invokeLater(this::cargarDetalles),
                    mensajeDetalle);
        } catch (IllegalArgumentException e) {
            mensaje(mensajeDetalle, e.getMessage(), ROJO);
        }
    }

    private Detalle detalleElegido() {
        int fila = tablaDetalles.getSelectedRow();
        return fila < 0 ? null : filasDetalles.get(tablaDetalles.convertRowIndexToModel(fila));
    }

    private void cambiarDetalle(boolean quitar) {
        Detalle elegido = detalleElegido();
        if (elegido == null || tareas.ocupado()) return;
        if (quitar && !confirmar("Quitar la asociación con " + elegido.nombre() + "?")) return;
        try {
            LocalDate anterior =
                    elegido.entidad().equals("software") && elegido.detalle() != null
                            ? LocalDate.parse(elegido.detalle())
                            : null;
            LocalDate nueva =
                    quitar || !elegido.entidad().equals("software")
                            ? null
                            : fecha(fechaInstalacion);
            limitar(detalleComponente, 200);
            String especificacion = detalleComponente.getText().strip();
            ejecutar(
                    () -> {
                        servicio.vincular(
                                equipoDetalles.id(),
                                elegido.entidad(),
                                elegido.clave(),
                                nueva,
                                quitar ? "quitar" : "modificar",
                                anterior,
                                especificacion,
                                elegido.entidad().equals("componente") ? elegido.detalle() : null);
                        return null;
                    },
                    nada -> SwingUtilities.invokeLater(this::cargarDetalles),
                    mensajeDetalle);
        } catch (IllegalArgumentException e) {
            mensaje(mensajeDetalle, e.getMessage(), ROJO);
        }
    }

    private record DatosDetalles(
            List<Registro> software, List<Registro> componentes, List<Detalle> detalles) {}
}
