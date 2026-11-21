package inventario.servicio;

import inventario.config.Conexion;
import inventario.dao.*;
import inventario.modelo.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Coordina las operaciones. Las pantallas no dependen de JDBC ni de las clases DAO.
public final class InventarioServicio {
    private final EquipoDao equipos;
    private final CatalogoDao catalogos;
    private final DetalleDao detalles;
    private final ReporteDao reportes;
    private final HistorialDao historial;

    public InventarioServicio(Conexion conexiones) {
        equipos = new EquipoDao(conexiones);
        catalogos = new CatalogoDao(conexiones);
        detalles = new DetalleDao(conexiones);
        reportes = new ReporteDao(conexiones);
        historial = new HistorialDao(conexiones);
    }

    public void verificar() throws SQLException {
        equipos.catalogos();
    }

    public List<Equipo> listar(String filtro, boolean inactivos) throws SQLException {
        return equipos.listar(filtro, inactivos);
    }

    public List<Catalogo> catalogos() throws SQLException {
        return equipos.catalogos();
    }

    public Optional<Ficha> ficha(String codigo) throws SQLException {
        return equipos.ficha(codigo);
    }

    public int guardar(Equipo equipo) throws SQLException {
        Validaciones.equipo(equipo);
        return equipos.guardar(equipo);
    }

    public void actividad(Equipo equipo, boolean activo) throws SQLException {
        equipos.actividad(equipo, activo);
    }

    public List<Registro> registros(String entidad, boolean inactivos) throws SQLException {
        Validaciones.entidad(entidad);
        return catalogos.registros(entidad, inactivos);
    }

    public String guardarRegistro(String entidad, Registro registro) throws SQLException {
        Validaciones.catalogo(entidad, registro);
        return catalogos.guardarRegistro(entidad, registro);
    }

    public void actividadRegistro(String entidad, Registro registro, boolean activo)
            throws SQLException {
        Validaciones.entidad(entidad);
        catalogos.actividadRegistro(entidad, registro, activo);
    }

    public List<Detalle> detalles(int equipo) throws SQLException {
        return detalles.detalles(equipo);
    }

    public void vincular(
            int equipo,
            String entidad,
            String clave,
            LocalDate fecha,
            String accion,
            LocalDate anterior,
            String detalle,
            String detalleAnterior)
            throws SQLException {
        Validaciones.entidad(entidad);
        Validaciones.texto(detalle, "Especificación", 200);
        detalles.vincular(
                equipo, entidad, clave, fecha, accion, anterior, detalle, detalleAnterior);
    }

    public Informe reporte(int numero, String filtro, LocalDate corte, int dias)
            throws SQLException {
        if (numero < 1 || numero > 15 || corte == null || dias < 1 || dias > 3650) {
            throw new IllegalArgumentException("Revisa el reporte, la fecha y el plazo indicado.");
        }
        return reportes.reporte(numero, filtro, corte, dias);
    }

    public List<Auditoria> auditoria(String filtro) throws SQLException {
        return historial.auditoria(filtro);
    }
}
