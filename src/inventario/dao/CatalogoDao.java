package inventario.dao;

import static inventario.dao.Consultas.*;

import inventario.config.Conexion;
import inventario.modelo.*;

import java.sql.*;
import java.util.*;

// Consulta y modifica los catálogos de software y componentes.
public final class CatalogoDao {
    private final Consultas consultas;

    public CatalogoDao(Conexion conexiones) {
        this.consultas = new Consultas(conexiones);
    }

    public List<Registro> registros(String entidad, boolean inactivos) throws SQLException {
        return consultas.leer(
                "sp_listar_catalogo",
                f ->
                        new Registro(
                                f.getString("clave"),
                                f.getString("nombre"),
                                f.getString("tipo"),
                                f.getString("version"),
                                f.getBoolean("activo"),
                                f.getInt("revision")),
                entidad,
                inactivos);
    }

    public String guardarRegistro(String entidad, Registro registro) throws SQLException {
        return consultas.escribirConTexto(
                "sp_guardar_catalogo",
                entidad,
                registro.clave(),
                registro.revision(),
                registro.nombre(),
                registro.tipo(),
                registro.version());
    }

    public void actividadRegistro(String entidad, Registro registro, boolean activo)
            throws SQLException {
        consultas.escribir(
                "sp_actividad_catalogo", entidad, registro.clave(), registro.revision(), activo);
    }
}
