package inventario.dao;

import static inventario.dao.Consultas.*;

import inventario.config.Conexion;
import inventario.modelo.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

// Lee los resultados de los reportes sin depender de sus columnas.
public final class ReporteDao {
    private final Conexion conexiones;

    public ReporteDao(Conexion conexiones) {
        this.conexiones = conexiones;
    }

    public Informe reporte(int numero, String filtro, LocalDate corte, int dias)
            throws SQLException {
        var columnas = new ArrayList<String>();
        var filas = new ArrayList<List<String>>();
        try (var conexion = conexiones.abrir()) {
            conexion.setReadOnly(true);
            conexion.setAutoCommit(false);
            try (var consulta =
                    conexion.prepareStatement(
                            "CALL public.sp_reporte(?, ?, ?, ?, 'reporte'::refcursor)")) {
                parametros(consulta, new Object[] {numero, filtro, corte, dias});
                consulta.execute();
            }
            try (var consulta = conexion.createStatement();
                    var f = consulta.executeQuery("FETCH ALL FROM reporte")) {
                var meta = f.getMetaData();
                for (int i = 1; i <= meta.getColumnCount(); i++)
                    columnas.add(meta.getColumnLabel(i));
                while (f.next()) {
                    var fila = new ArrayList<String>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) fila.add(f.getString(i));
                    filas.add(fila);
                }
            }
            conexion.commit();
        }
        return new Informe(columnas, filas);
    }
}
