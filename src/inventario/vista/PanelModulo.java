package inventario.vista;

import inventario.servicio.InventarioServicio;

import java.awt.BorderLayout;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.swing.*;

// Comparte el servicio y las tareas; cada pantalla conserva su propio estado.
abstract class PanelModulo extends JPanel {
    protected final InventarioServicio servicio;
    protected final EjecutorTareas tareas;

    protected PanelModulo(InventarioServicio servicio, EjecutorTareas tareas) {
        super(new BorderLayout());
        this.servicio = servicio;
        this.tareas = tareas;
        tareas.alCambiar(this::actualizarControles);
    }

    public void cargar() {}

    protected abstract void actualizarControles();

    protected final <T> void ejecutar(Callable<T> accion, Consumer<T> exito, JLabel mensaje) {
        tareas.ejecutar(accion, exito, mensaje);
    }

    protected final boolean confirmar(String texto) {
        return JOptionPane.showConfirmDialog(
                        this,
                        texto,
                        "Confirmar operación",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE)
                == JOptionPane.YES_OPTION;
    }
}
