package inventario.dao;

import inventario.config.Conexion;

import java.sql.*;
import java.util.*;

// Comparte las llamadas JDBC y cierra cada conexión al terminar.
final class Consultas {
    private static final String CURSOR = "inventario_resultado";
    private final Conexion conexiones;

    Consultas(Conexion conexiones) {
        this.conexiones = conexiones;
    }

    @FunctionalInterface
    interface MapaFila<T> {
        T crear(ResultSet fila) throws SQLException;
    }

    // El cursor se lee antes de cerrar la transacción.
    <T> List<T> leer(String nombre, MapaFila<T> mapa, Object... parametros) throws SQLException {
        var registros = new ArrayList<T>();
        try (var conexion = conexiones.abrir()) {
            conexion.setReadOnly(true);
            conexion.setAutoCommit(false);
            try (var consulta =
                    conexion.prepareStatement(
                            llamada(nombre, parametros.length, "'" + CURSOR + "'::refcursor"))) {
                parametros(consulta, parametros);
                consulta.execute();
            }
            try (var consulta = conexion.createStatement();
                    var filas = consulta.executeQuery("FETCH ALL FROM " + CURSOR)) {
                while (filas.next()) registros.add(mapa.crear(filas));
            }
            conexion.commit();
        }
        return registros;
    }

    Number escribirConSalida(String nombre, Object... parametros) throws SQLException {
        return (Number) modificar(nombre, true, parametros);
    }

    String escribirConTexto(String nombre, Object... parametros) throws SQLException {
        return modificar(nombre, true, parametros).toString();
    }

    void escribir(String nombre, Object... parametros) throws SQLException {
        modificar(nombre, false, parametros);
    }

    // Guarda el cambio y su historial juntos; deshace ambos si ocurre un error.
    private Object modificar(String nombre, boolean salida, Object... parametros)
            throws SQLException {
        try (var conexion = conexiones.abrir()) {
            conexion.setAutoCommit(false);
            try (var consulta =
                    conexion.prepareStatement(
                            llamada(nombre, parametros.length, salida ? "NULL" : ""))) {
                parametros(consulta, parametros);
                consulta.execute();
                Object valor = null;
                if (salida) {
                    try (var fila = consulta.getResultSet()) {
                        if (!fila.next())
                            throw new SQLException("El procedimiento no devolvió el resultado.");
                        valor = fila.getObject(1);
                    }
                }
                conexion.commit();
                return valor;
            } catch (SQLException error) {
                try {
                    conexion.rollback();
                } catch (SQLException rollback) {
                    error.addSuppressed(rollback);
                }
                throw error;
            }
        }
    }

    private static String llamada(String nombre, int cantidad, String ultimo) {
        var argumentos = new ArrayList<>(Collections.nCopies(cantidad, "?"));
        if (!ultimo.isEmpty()) argumentos.add(ultimo);
        return "CALL public." + nombre + "(" + String.join(", ", argumentos) + ")";
    }

    static void parametros(PreparedStatement consulta, Object[] valores) throws SQLException {
        for (int i = 0; i < valores.length; i++) consulta.setObject(i + 1, valores[i]);
    }

    static Integer entero(ResultSet fila, String columna) throws SQLException {
        return fila.getObject(columna, Integer.class);
    }

    static List<String> arreglo(ResultSet fila, String columna) throws SQLException {
        var valor = fila.getArray(columna);
        if (valor == null) return List.of();
        try {
            return List.of((String[]) valor.getArray());
        } finally {
            valor.free();
        }
    }

    static String texto(String valor) {
        return valor == null ? "" : valor;
    }
}
