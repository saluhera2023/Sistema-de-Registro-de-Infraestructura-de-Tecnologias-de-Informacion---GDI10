package inventario.vista;

import inventario.util.Errores;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.JLabel;
import javax.swing.SwingWorker;

// Realiza las consultas en segundo plano y avisa cuándo cambian los controles.
public final class EjecutorTareas {
    private boolean ocupado;
    private final List<Runnable> observadores = new ArrayList<>();

    public boolean ocupado() {
        return ocupado;
    }

    public void alCambiar(Runnable accion) {
        observadores.add(accion);
    }

    private void avisar() {
        for (Runnable accion : observadores) accion.run();
    }

    public <T> void ejecutar(Callable<T> accion, Consumer<T> exito, JLabel mensaje) {
        if (ocupado) return;
        ocupado = true;
        avisar();
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return accion.call();
            }

            @Override
            protected void done() {
                try {
                    exito.accept(get());
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    Componentes.mensaje(mensaje, "La operación se interrumpió.", Componentes.ROJO);
                } catch (ExecutionException error) {
                    Componentes.mensaje(
                            mensaje, Errores.describir(error.getCause()), Componentes.ROJO);
                } finally {
                    ocupado = false;
                    avisar();
                }
            }
        }.execute();
    }
}
