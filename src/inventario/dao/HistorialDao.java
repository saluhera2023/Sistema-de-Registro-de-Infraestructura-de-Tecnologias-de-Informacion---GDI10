package inventario.dao;

import static inventario.dao.Consultas.*;

import inventario.config.Conexion;
import inventario.modelo.*;

import java.sql.*;
import java.util.*;

// Recupera el historial mediante su procedimiento almacenado.
public final class HistorialDao {
    private final Consultas consultas;

    public HistorialDao(Conexion conexiones) {
        this.consultas = new Consultas(conexiones);
    }

    public List<Auditoria> auditoria(String filtro) throws SQLException {
        return consultas.leer(
                "sp_auditoria_inventario",
                f ->
                        new Auditoria(
                                f.getLong("id_auditoria"),
                                f.getString("fecha"),
                                f.getString("usuario"),
                                f.getString("tabla"),
                                f.getString("operacion"),
                                f.getString("clave"),
                                f.getString("anterior"),
                                f.getString("nuevo")),
                filtro);
    }
}
