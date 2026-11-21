package inventario.config;

import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.SQLException;

// Abre conexiones JDBC. No conoce formularios ni reglas del inventario.
public final class Conexion {
    private final Configuracion configuracion;

    public Conexion(Configuracion configuracion) {
        this.configuracion = configuracion;
    }

    public Connection abrir() throws SQLException {
        return abrirEn(configuracion.base());
    }

    public Connection abrirEn(String base) throws SQLException {
        var origen = new PGSimpleDataSource();
        origen.setServerNames(new String[] {configuracion.servidor()});
        origen.setPortNumbers(new int[] {configuracion.puerto()});
        origen.setDatabaseName(base);
        origen.setUser(configuracion.usuario());
        origen.setPassword(configuracion.clave());
        origen.setConnectTimeout(5);
        origen.setSocketTimeout(30);
        return origen.getConnection();
    }
}
