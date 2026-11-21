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

// Catálogo independiente; comparte la misma vista para software y componentes.
public final class CatalogoPanel extends PanelModulo {
    private final String entidad;
    private final boolean esSoftware;
    private final JTable listado;
    private final JTextField nombreRegistro, tipoRegistro, versionRegistro;
    private final JCheckBox incluir;
    private final JButton refrescar, crear, editarRegistro, guardarRegistro, actividadRegistro;
    private final JLabel mensajeRegistro;
    private Registro editando;
    private List<Registro> registros = List.of();

    public CatalogoPanel(
            InventarioServicio servicio, EjecutorTareas tareas, String entidad, String titulo) {
        super(servicio, tareas);
        this.entidad = entidad;
        esSoftware = entidad.equals("software");
        listado =
                tabla(
                        entidad + "Tabla",
                        esSoftware
                                ? new String[] {"Nombre", "Tipo", "Versión", "Registro"}
                                : new String[] {"ID", "Componente", "Registro"});
        nombreRegistro = campo(entidad + "Nombre", "");
        tipoRegistro = campo(entidad + "Tipo", "");
        versionRegistro = campo(entidad + "Version", "");
        incluir = new JCheckBox("Incluir inactivos");
        incluir.setName(entidad + "Inactivos");
        incluir.setOpaque(false);
        refrescar = boton(entidad + "Refrescar", "Actualizar listado", false);
        crear = boton(entidad + "Nuevo", "Nuevo", true);
        editarRegistro = boton(entidad + "Editar", "Editar selección", false);
        guardarRegistro = boton(entidad + "Guardar", "Guardar", true);
        actividadRegistro = boton(entidad + "Actividad", "Desactivar", false);
        mensajeRegistro =
                aviso(entidad + "Mensaje", "Elige Nuevo o selecciona una fila para editar.");
        var panel =
                pantalla(
                        titulo, "Registra, consulta, modifica o desactiva los datos del catálogo.");
        var cuerpo = new JPanel(new BorderLayout(0, 10));
        cuerpo.setOpaque(false);
        cuerpo.add(
                botones(crear, refrescar, incluir, editarRegistro, actividadRegistro),
                BorderLayout.NORTH);
        cuerpo.add(new JScrollPane(listado));
        var formulario = formulario();
        fila(formulario, 0, "Nombre *", nombreRegistro);
        if (esSoftware) {
            fila(formulario, 1, "Tipo", tipoRegistro);
            fila(formulario, 2, "Versión", versionRegistro);
        }
        fila(formulario, 3, "", guardarRegistro);
        var pie = columna();
        pie.add(formulario);
        pie.add(Box.createVerticalStrut(8));
        pie.add(mensajeRegistro);
        cuerpo.add(pie, BorderLayout.SOUTH);
        panel.add(cuerpo);
        add(panel);
        refrescar.addActionListener(e -> cargar());
        incluir.addActionListener(e -> cargar());
        crear.addActionListener(e -> reiniciar());
        editarRegistro.addActionListener(e -> editar());
        guardarRegistro.addActionListener(e -> guardar());
        actividadRegistro.addActionListener(e -> actividad());
        listado.getSelectionModel().addListSelectionListener(e -> actualizarControles());
    }

    private Registro seleccionado() {
        int fila = listado.getSelectedRow();
        return fila < 0 ? null : registros.get(listado.convertRowIndexToModel(fila));
    }

    @Override
    public void cargar() {
        boolean inactivos = incluir.isSelected();
        ejecutar(
                () -> servicio.registros(entidad, inactivos),
                filas -> {
                    registros = filas;
                    modeloTabla(listado).setRowCount(0);
                    for (var fila : filas)
                        modeloTabla(listado)
                                .addRow(
                                        esSoftware
                                                ? new Object[] {
                                                    fila.nombre(),
                                                    valor(fila.tipo()),
                                                    valor(fila.version()),
                                                    fila.activo() ? "Activo" : "Inactivo"
                                                }
                                                : new Object[] {
                                                    fila.clave(),
                                                    fila.nombre(),
                                                    fila.activo() ? "Activo" : "Inactivo"
                                                });
                    reiniciar();
                    mensaje(mensajeRegistro, filas.size() + " registros recuperados.", SUAVE);
                },
                mensajeRegistro);
    }

    private void reiniciar() {
        editando = null;
        nombreRegistro.setText("");
        tipoRegistro.setText("");
        versionRegistro.setText("");
        nombreRegistro.setEditable(true);
        mensaje(mensajeRegistro, "Completa el formulario para crear un registro.", SUAVE);
    }

    private void editar() {
        editando = seleccionado();
        if (editando == null) return;
        nombreRegistro.setText(editando.nombre());
        tipoRegistro.setText(vacio(editando.tipo()));
        versionRegistro.setText(vacio(editando.version()));
        nombreRegistro.setEditable(true);
        mensaje(mensajeRegistro, "Modifica los datos y pulsa Guardar.", SUAVE);
    }

    private void guardar() {
        if (tareas.ocupado()) return;
        try {
            exigir(nombreRegistro, "Nombre", esSoftware ? 120 : 100);
            limitar(tipoRegistro, 80);
            limitar(versionRegistro, 60);
            var registro =
                    new Registro(
                            editando == null ? null : editando.clave(),
                            nombreRegistro.getText().strip(),
                            tipoRegistro.getText().strip(),
                            versionRegistro.getText().strip(),
                            true,
                            editando == null ? 1 : editando.revision());
            ejecutar(
                    () -> {
                        String clave = servicio.guardarRegistro(entidad, registro);
                        return servicio.registros(entidad, false).stream()
                                .filter(r -> r.clave().equals(clave))
                                .findFirst()
                                .orElseThrow();
                    },
                    guardado -> {
                        editando = guardado;
                        nombreRegistro.setEditable(true);
                        mensaje(mensajeRegistro, "Registro guardado correctamente.", VERDE);
                        SwingUtilities.invokeLater(this::cargar);
                    },
                    mensajeRegistro);
        } catch (IllegalArgumentException e) {
            mensaje(mensajeRegistro, e.getMessage(), ROJO);
        }
    }

    private void actividad() {
        Registro registro = seleccionado();
        if (registro == null || tareas.ocupado()) return;
        if (!confirmar(
                (registro.activo() ? "Desactivar " : "Reactivar ") + registro.nombre() + "?"))
            return;
        ejecutar(
                () -> {
                    servicio.actividadRegistro(entidad, registro, !registro.activo());
                    return null;
                },
                nada -> SwingUtilities.invokeLater(this::cargar),
                mensajeRegistro);
    }

    private void controles(boolean listo) {
        Registro elegido = seleccionado();
        refrescar.setEnabled(listo);
        crear.setEnabled(listo);
        guardarRegistro.setEnabled(listo);
        editarRegistro.setEnabled(listo && elegido != null && elegido.activo());
        actividadRegistro.setEnabled(listo && elegido != null);
        actividadRegistro.setText(
                elegido != null && !elegido.activo() ? "Reactivar" : "Desactivar");
    }

    @Override
    protected void actualizarControles() {
        controles(!tareas.ocupado());
    }
}
