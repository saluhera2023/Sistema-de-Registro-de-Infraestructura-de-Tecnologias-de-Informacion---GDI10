package inventario.dao;

import static inventario.dao.Consultas.*;

import inventario.config.Conexion;
import inventario.modelo.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

// Guarda las instalaciones y las especificaciones de cada equipo.
public final class DetalleDao {
    private final Consultas consultas;

    public DetalleDao(Conexion conexiones) {
        this.consultas = new Consultas(conexiones);
    }

    public List<Detalle> detalles(int equipo) throws SQLException {
        return consultas.leer(
                "sp_listar_detalles",
                f ->
                        new Detalle(
                                f.getString("entidad"),
                                f.getString("clave"),
                                f.getString("nombre"),
                                f.getString("detalle")),
                equipo);
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
        consultas.escribir(
                "sp_vincular_detalle",
                equipo,
                entidad,
                clave,
                fecha,
                accion,
                anterior,
                detalle,
                detalleAnterior);
    }
}
