package inventario.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

// Prepara una base vacía desde la terminal y conserva las que ya están instaladas.
public final class PrepararBase {
    private PrepararBase() {}

    public static String ejecutar(Configuracion configuracion, Path script)
            throws SQLException, IOException {
        String sql = Files.readString(script, StandardCharsets.UTF_8);
        var conexiones = new Conexion(configuracion);
        try (var conexion = conexiones.abrirEn("postgres")) {
            boolean existe;
            try (var consulta =
                    conexion.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
                consulta.setString(1, configuracion.base());
                try (var filas = consulta.executeQuery()) {
                    existe = filas.next();
                }
            }
            if (!existe) {
                // El nombre se cita como identificador; nunca se trata como SQL recibido del
                // usuario.
                String nombre = "\"" + configuracion.base().replace("\"", "\"\"") + "\"";
                try (var consulta = conexion.createStatement()) {
                    consulta.executeUpdate("CREATE DATABASE " + nombre);
                }
            }
        }
        try (var conexion = conexiones.abrir();
                var consulta = conexion.createStatement()) {
            try (var filas =
                    consulta.executeQuery(
                            "SELECT count(*) FROM pg_tables WHERE schemaname='public'")) {
                filas.next();
                if (filas.getInt(1) > 0) {
                    try (var revision = conexion.createStatement();
                            var estado =
                                    revision.executeQuery(
                                            """
                        SELECT to_regprocedure('public.sp_listar_inventario(text,boolean,refcursor)') IS NOT NULL
                           AND to_regprocedure('public.sp_guardar_catalogo(text,text,integer,text,text,text)') IS NOT NULL
                           AND to_regprocedure('public.sp_vincular_detalle(integer,text,text,date,text,date,text,text)') IS NOT NULL
   AND to_regprocedure('public.sp_reporte(integer,text,date,integer,refcursor)') IS NOT NULL
                           AND to_regprocedure('public.sp_guardar_catalogo(text,text,integer,text,text,text)') IS NOT NULL
                           AND to_regprocedure('public.sp_vincular_detalle(integer,text,text,date,text,date,text,text)') IS NOT NULL
   AND EXISTS(SELECT FROM information_schema.columns WHERE table_schema='public'
       AND table_name='equipo_componente' AND column_name='detalle')
   AND EXISTS(SELECT FROM information_schema.columns WHERE table_schema='public'
       AND table_name='componente' AND column_name='id_componente')
""")) {
                        estado.next();
                        if (estado.getBoolean(1))
                            return "La base ya está preparada. Se conservaron sus registros.";
                    }
                    throw new IllegalStateException(
                            "La base contiene tablas de otra estructura. Usa una base vacía para"
                                    + " instalar; no se modificaron los registros.");
                }
            }
            consulta.execute(sql);
            return "Base preparada correctamente con 90 equipos de prueba.";
        }
    }
}
