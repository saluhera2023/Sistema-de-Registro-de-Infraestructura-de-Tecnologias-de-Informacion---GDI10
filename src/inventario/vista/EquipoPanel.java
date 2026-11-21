package inventario.vista;

import static inventario.vista.Componentes.*;

import inventario.modelo.*;
import inventario.servicio.InventarioServicio;

import java.awt.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

import javax.swing.*;

// Formulario de alta y edición de equipos.
public final class EquipoPanel extends PanelModulo {
    private Equipo enEdicion;
    private final JLabel tituloFormulario = texto("Registrar equipo", 22, true, TINTA);
    private final JTextField codigo = campo("codigo", ""),
            nombre = campo("nombre", ""),
            modelo = campo("modelo", ""),
            serie = campo("serie", ""),
            ip = campo("ip", ""),
            compra = campo("compra", ""),
            garantia = campo("garantia", "");
    private final JComboBox<Catalogo> tipo = combo("tipo"),
            estado = combo("estado"),
            oficina = combo("oficina"),
            area = combo("area"),
            empleado = combo("empleado"),
            marca = combo("marca"),
            principal = combo("principal");
    private final JButton guardar = boton("guardar", "Guardar equipo", true),
            limpiar = boton("limpiar", "Nuevo formulario", false);
    private final JLabel mensajeAlta =
            aviso("mensajeAlta", "Completa el código, el tipo y el estado.");

    public EquipoPanel(InventarioServicio servicio, EjecutorTareas tareas) {
        super(servicio, tareas);
        add(pantallaEquipo());
        guardar.addActionListener(e -> guardarEquipo());
        limpiar.addActionListener(
                e -> {
                    enEdicion = null;
                    cargar();
                });
        actualizarControles();
    }

    public void seleccionarEquipo(Equipo equipo) {
        enEdicion = equipo;
    }

    @Override
    public void cargar() {
        cargarFormulario();
    }

    @Override
    protected void actualizarControles() {
        guardar.setEnabled(!tareas.ocupado());
        limpiar.setEnabled(!tareas.ocupado());
    }

    private JPanel pantallaEquipo() {
        var panel =
                pantalla(
                        "Equipo",
                        "Completa los campos obligatorios con *. Las fechas usan AAAA-MM-DD.");
        var izquierdo = formulario();
        fila(izquierdo, 0, "Código de inventario *", codigo);
        fila(izquierdo, 1, "Nombre", nombre);
        fila(izquierdo, 2, "Modelo", modelo);
        fila(izquierdo, 3, "Número de serie", serie);
        fila(izquierdo, 4, "Tipo *", tipo);
        fila(izquierdo, 5, "Estado *", estado);
        fila(izquierdo, 6, "Marca", marca);
        var derecho = formulario();
        fila(derecho, 0, "Oficina", oficina);
        fila(derecho, 1, "Área", area);
        fila(derecho, 2, "Asignado a", empleado);
        fila(derecho, 3, "Equipo principal", principal);
        fila(derecho, 4, "Dirección IP", ip);
        fila(derecho, 5, "Fecha de compra", compra);
        fila(derecho, 6, "Garantía hasta", garantia);
        var cuerpo = new JPanel(new BorderLayout(0, 15));
        cuerpo.setOpaque(false);
        cuerpo.add(tituloFormulario, BorderLayout.NORTH);
        cuerpo.add(dosColumnas(izquierdo, derecho));
        var pie = columna();
        pie.add(botones(guardar, limpiar));
        pie.add(Box.createVerticalStrut(10));
        pie.add(mensajeAlta);
        cuerpo.add(pie, BorderLayout.SOUTH);
        panel.add(cuerpo);
        return panel;
    }

    private void cargarFormulario() {
        Equipo seleccionado = enEdicion;
        ejecutar(
                servicio::catalogos,
                filas -> {
                    llenar(tipo, filas, "tipo", "Selecciona el tipo", null);
                    llenar(estado, filas, "estado", "Selecciona el estado", null);
                    llenar(oficina, filas, "oficina", "Sin consignar", null);
                    llenar(area, filas, "area", "Sin consignar", null);
                    llenar(empleado, filas, "empleado", "Sin consignar", null);
                    llenar(marca, filas, "marca", "Sin consignar", null);
                    llenar(
                            principal,
                            filas,
                            "principal",
                            "Sin conexión a otro equipo",
                            seleccionado == null ? null : seleccionado.id());
                    codigo.setText(seleccionado == null ? "" : seleccionado.codigo());
                    nombre.setText(seleccionado == null ? "" : vacio(seleccionado.nombre()));
                    modelo.setText(seleccionado == null ? "" : vacio(seleccionado.modelo()));
                    serie.setText(seleccionado == null ? "" : vacio(seleccionado.serie()));
                    ip.setText(seleccionado == null ? "" : vacio(seleccionado.ip()));
                    compra.setText(seleccionado == null ? "" : fecha(seleccionado.compra()));
                    garantia.setText(seleccionado == null ? "" : fecha(seleccionado.garantia()));
                    if (seleccionado != null) {
                        seleccionar(tipo, seleccionado.tipo());
                        seleccionar(estado, seleccionado.estado());
                        seleccionar(oficina, seleccionado.oficina());
                        seleccionar(area, seleccionado.area());
                        seleccionar(empleado, seleccionado.empleado());
                        seleccionar(marca, seleccionado.marca());
                        seleccionar(principal, seleccionado.principal());
                    }
                    tituloFormulario.setText(
                            seleccionado == null
                                    ? "Registrar equipo"
                                    : "Editar " + seleccionado.codigo());
                    mensaje(
                            mensajeAlta,
                            seleccionado == null
                                    ? "Completa los campos obligatorios."
                                    : "Modifica los datos y pulsa Guardar equipo.",
                            SUAVE);
                },
                mensajeAlta);
    }

    private void guardarEquipo() {
        if (tareas.ocupado()) return;
        try {
            exigir(codigo, "Código", 20);
            limitar(nombre, 80);
            limitar(modelo, 80);
            limitar(serie, 80);
            if (id(tipo) == null || id(estado) == null)
                throw new IllegalArgumentException("Selecciona tipo y estado.");
            var valores =
                    new Equipo(
                            enEdicion == null ? null : enEdicion.id(),
                            enEdicion == null ? 1 : enEdicion.revision(),
                            true,
                            codigo.getText().strip(),
                            nombre.getText().strip(),
                            modelo.getText().strip(),
                            serie.getText().strip(),
                            id(tipo),
                            id(estado),
                            id(oficina),
                            id(area),
                            id(empleado),
                            id(marca),
                            id(principal),
                            ip.getText().strip(),
                            fecha(compra),
                            fecha(garantia),
                            "",
                            "",
                            "",
                            "");
            ejecutar(
                    () -> {
                        servicio.guardar(valores);
                        return servicio.listar(valores.codigo(), false).stream()
                                .filter(e -> e.codigo().equals(valores.codigo()))
                                .findFirst()
                                .orElseThrow();
                    },
                    guardado -> {
                        enEdicion = guardado;
                        tituloFormulario.setText("Editar " + guardado.codigo());
                        mensaje(
                                mensajeAlta,
                                "Equipo " + guardado.codigo() + " guardado correctamente.",
                                VERDE);
                    },
                    mensajeAlta);
        } catch (IllegalArgumentException e) {
            mensaje(mensajeAlta, e.getMessage(), ROJO);
        }
    }
}
