package inventario;

import inventario.config.*;
import inventario.servicio.InventarioServicio;
import inventario.util.Errores;
import inventario.vista.VentanaPrincipal;

import java.nio.file.Path;

import javax.swing.SwingUtilities;

// Punto de entrada para la ventana y para las comprobaciones de terminal.
public final class App {
    private App() {}

    public static void main(String[] args) {
        try {
            Path carpeta = Path.of(".").toAbsolutePath().normalize();
            Path carpetaConfiguracion = Path.of(System.getProperty("inventario.config.dir", carpeta.toString()));
            Configuracion configuracion = Configuracion.cargar(carpetaConfiguracion);
            String accion = args.length == 0 ? "iniciar" : args[0];
            if (accion.equals("--preparar")) {
                System.out.println(
                        PrepararBase.ejecutar(
                                configuracion, carpeta.resolve("sql/inventario.sql")));
                return;
            }
            var servicio = new InventarioServicio(new Conexion(configuracion));
            servicio.verificar();
            if (accion.equals("--verificar")) {
                System.out.println("Conexión correcta con la base " + configuracion.base() + ".");
                return;
            }
            if (!accion.equals("iniciar"))
                throw new IllegalArgumentException("Acción desconocida: " + accion);
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
            SwingUtilities.invokeLater(() -> new VentanaPrincipal(servicio).setVisible(true));
        } catch (Exception error) {
            String mensaje =
                    error instanceof java.sql.SQLException
                            ? Errores.describir(error)
                            : error.getMessage();
            System.err.println("No se pudo iniciar: " + mensaje);
            System.exit(1);
        }
    }
}
