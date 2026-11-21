package inventario.vista;

import static inventario.vista.Componentes.*;

import inventario.modelo.Equipo;
import inventario.servicio.InventarioServicio;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

// Organiza las pestañas y la navegación. No ejecuta SQL ni contiene formularios.
public final class VentanaPrincipal extends JFrame {
    public static final String NOMBRE_APLICATIVO =
            "SISTEMA DE REGISTRO DE INFRAESTRUCTURA DE TECNOLOGÍAS DE LA INFORMACIÓN";
    private final JTabbedPane pestanas = new JTabbedPane();
    private final EjecutorTareas tareas = new EjecutorTareas();
    private final EquipoPanel equipo;
    private final DetallesPanel detalles;

    public VentanaPrincipal(InventarioServicio servicio) {
        super(NOMBRE_APLICATIVO);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 720));
        setSize(1190, 820);
        setLocationRelativeTo(null);
        add(cabecera(), BorderLayout.NORTH);
        equipo = new EquipoPanel(servicio, tareas);
        detalles = new DetallesPanel(servicio, tareas);
        var inventario =
                new InventarioPanel(servicio, tareas, this::abrirEquipo, this::abrirDetalles);
        pestanas.setName("pestanas");
        pestanas.setFont(new Font("Dialog", Font.BOLD, 14));
        pestanas.addTab("Inventario", inventario);
        pestanas.addTab("Equipo", equipo);
        pestanas.addTab("Software", new CatalogoPanel(servicio, tareas, "software", "Software"));
        pestanas.addTab(
                "Componentes", new CatalogoPanel(servicio, tareas, "componente", "Componentes"));
        pestanas.addTab("Detalles del equipo", detalles);
        pestanas.addTab("Reportes", new ReportesPanel(servicio, tareas));
        pestanas.addTab("Historial", new HistorialPanel(servicio, tareas));
        var centro = new JPanel(new BorderLayout());
        centro.setBackground(FONDO);
        centro.setBorder(new EmptyBorder(14, 22, 18, 22));
        centro.add(pestanas);
        add(centro);
        tareas.alCambiar(
                () -> {
                    for (int i = 0; i < pestanas.getTabCount(); i++)
                        pestanas.setEnabledAt(i, !tareas.ocupado());
                    setCursor(
                            Cursor.getPredefinedCursor(
                                    tareas.ocupado() ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
                });
        pestanas.addChangeListener(
                e -> {
                    if (!tareas.ocupado()) ((PanelModulo) pestanas.getSelectedComponent()).cargar();
                });
        inventario.cargar();
    }

    private void abrirEquipo(Equipo seleccionado) {
        equipo.seleccionarEquipo(seleccionado);
        pestanas.setSelectedComponent(equipo);
    }

    private void abrirDetalles(Equipo seleccionado) {
        detalles.seleccionar(seleccionado);
        pestanas.setSelectedComponent(detalles);
    }

    private JPanel cabecera() {
        var panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(18, 25, 18, 25));
        panel.setBackground(TINTA);
        var titulo =
                texto(
                        "<html>SISTEMA DE REGISTRO DE INFRAESTRUCTURA<br>DE TECNOLOGÍAS DE LA"
                            + " INFORMACIÓN</html>",
                        24,
                        true,
                        Color.WHITE);
        titulo.setName("nombreAplicativo");
        panel.add(titulo);
        return panel;
    }
}
