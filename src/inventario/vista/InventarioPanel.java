package inventario.vista;

import static inventario.vista.Componentes.*;

import inventario.modelo.*;
import inventario.servicio.InventarioServicio;

import java.awt.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.*;

// Listado y hoja de vida. Avisa a la ventana cuando se quiere abrir otro módulo.
public final class InventarioPanel extends PanelModulo {
    private final JTextField filtro = campo("filtro", "");
    private final JCheckBox inactivos = new JCheckBox("Incluir inactivos");
    private final JButton buscar = boton("buscar", "Buscar", true),
            nuevo = boton("nuevo", "Nuevo equipo", true),
            editar = boton("editar", "Editar", false),
            actividad = boton("actividad", "Desactivar", false),
            verFicha = boton("verFicha", "Hoja de vida", false),
            detalles = boton("detalles", "Software y componentes", false),
            volver = boton("volver", "Volver al inventario", false);
    private final JLabel mensajeLista = aviso("mensajeLista", "Consulta el inventario.");
    private final JTable tabla =
            tabla("tabla", "Código", "Tipo", "Modelo", "Estado", "Oficina", "Registro");
    private List<Equipo> equipos = List.of();
    private final CardLayout vistas = new CardLayout();
    private final JPanel inventario = new JPanel(vistas);
    private final JEditorPane contenidoFicha = new JEditorPane("text/html", "");

    public InventarioPanel(
            InventarioServicio servicio,
            EjecutorTareas tareas,
            Consumer<Equipo> alEditar,
            Consumer<Equipo> alVerDetalles) {
        super(servicio, tareas);
        add(pantallaInventario());
        buscar.addActionListener(e -> cargar());
        filtro.addActionListener(e -> cargar());
        inactivos.addActionListener(e -> cargar());
        nuevo.addActionListener(e -> alEditar.accept(null));
        editar.addActionListener(e -> alEditar.accept(elegido()));
        actividad.addActionListener(e -> actividadEquipo());
        verFicha.addActionListener(e -> mostrarFicha());
        volver.addActionListener(e -> vistas.show(inventario, "lista"));
        detalles.addActionListener(e -> alVerDetalles.accept(elegido()));
        tabla.getSelectionModel().addListSelectionListener(e -> actualizarControles());
        actualizarControles();
    }

    @Override
    public void cargar() {
        vistas.show(inventario, "lista");
        cargarLista();
    }

    @Override
    protected void actualizarControles() {
        boolean listo = !tareas.ocupado();
        Equipo elegido = elegido();
        buscar.setEnabled(listo);
        nuevo.setEnabled(listo);
        volver.setEnabled(listo);
        editar.setEnabled(listo && elegido != null && elegido.activo());
        verFicha.setEnabled(listo && elegido != null && elegido.activo());
        detalles.setEnabled(listo && elegido != null && elegido.activo());
        actividad.setEnabled(listo && elegido != null);
        actividad.setText(elegido != null && !elegido.activo() ? "Reactivar" : "Desactivar");
    }

    private JPanel pantallaInventario() {
        var lista =
                pantalla(
                        "Equipos registrados",
                        "Busca por código, nombre o modelo y selecciona un equipo para operar.");
        inactivos.setName("inactivos");
        inactivos.setOpaque(false);
        var herramientas = new JPanel(new BorderLayout(10, 10));
        herramientas.setOpaque(false);
        var busqueda = new JPanel(new BorderLayout(10, 0));
        busqueda.setOpaque(false);
        busqueda.add(filtro);
        busqueda.add(botones(buscar, inactivos), BorderLayout.EAST);
        herramientas.add(busqueda, BorderLayout.NORTH);
        herramientas.add(botones(nuevo, editar, actividad, verFicha, detalles), BorderLayout.SOUTH);
        var contenido = new JPanel(new BorderLayout(0, 10));
        contenido.setOpaque(false);
        contenido.add(herramientas, BorderLayout.NORTH);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(310);
        contenido.add(new JScrollPane(tabla));
        contenido.add(mensajeLista, BorderLayout.SOUTH);
        lista.add(contenido);
        var ficha =
                pantalla(
                        "Hoja de vida del equipo",
                        "Identificación, ubicación, responsable y detalles registrados.");
        var cuerpo = new JPanel(new BorderLayout(0, 8));
        cuerpo.setOpaque(false);
        cuerpo.add(botones(volver), BorderLayout.NORTH);
        contenidoFicha.setName("contenidoFicha");
        contenidoFicha.setEditable(false);
        cuerpo.add(new JScrollPane(contenidoFicha));
        ficha.add(cuerpo);
        inventario.add(lista, "lista");
        inventario.add(ficha, "ficha");
        return inventario;
    }

    private void cargarLista() {
        if (tareas.ocupado()) return;
        String termino = filtro.getText().strip();
        boolean incluir = inactivos.isSelected();
        modeloTabla(tabla).setRowCount(0);
        mensaje(mensajeLista, "Consultando el inventario…", SUAVE);
        ejecutar(
                () -> servicio.listar(termino, incluir),
                filas -> {
                    equipos = filas;
                    for (var equipo : filas)
                        modeloTabla(tabla)
                                .addRow(
                                        new Object[] {
                                            equipo.codigo(),
                                            equipo.tipoTexto(),
                                            valor(equipo.modelo()),
                                            equipo.estadoTexto(),
                                            valor(equipo.oficinaTexto()),
                                            equipo.activo() ? "Activo" : "Inactivo"
                                        });
                    mensaje(mensajeLista, filas.size() + " equipos encontrados.", SUAVE);
                },
                mensajeLista);
    }

    private Equipo elegido() {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? null : equipos.get(tabla.convertRowIndexToModel(fila));
    }

    private void actividadEquipo() {
        Equipo equipo = elegido();
        if (equipo == null || tareas.ocupado()) return;
        boolean activar = !equipo.activo();
        if (!confirmar((activar ? "Reactivar " : "Desactivar ") + equipo.codigo() + "?")) return;
        ejecutar(
                () -> {
                    servicio.actividad(equipo, activar);
                    return null;
                },
                nada -> {
                    mensaje(
                            mensajeLista,
                            "Equipo "
                                    + equipo.codigo()
                                    + (activar ? " reactivado." : " desactivado."),
                            VERDE);
                    equipos = List.of();
                    modeloTabla(tabla).setRowCount(0);
                    SwingUtilities.invokeLater(this::cargarLista);
                },
                mensajeLista);
    }

    private void mostrarFicha() {
        Equipo equipo = elegido();
        if (equipo == null || tareas.ocupado()) return;
        ejecutar(
                () -> servicio.ficha(equipo.codigo()),
                ficha -> {
                    if (ficha.isEmpty()) {
                        mensaje(
                                mensajeLista,
                                "El equipo está inactivo o cambió. Actualiza la consulta.",
                                ROJO);
                        return;
                    }
                    contenidoFicha.setText(htmlFicha(ficha.orElseThrow()));
                    contenidoFicha.setCaretPosition(0);
                    vistas.show(inventario, "ficha");
                },
                mensajeLista);
    }
}
