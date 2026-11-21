package inventario.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

// Lee únicamente los datos necesarios para acceder a PostgreSQL.
public record Configuracion(
        String servidor, int puerto, String base, String usuario, String clave) {
    public static Configuracion cargar(Path carpeta) throws IOException {
        Path archivo = carpeta.resolve("configuracion.properties");
        if (!Files.isRegularFile(archivo)) {
            throw new IOException(
                    "Copia configuracion.ejemplo.properties como configuracion.properties y"
                        + " completa tu contraseña.");
        }
        var datos = new Properties();
        try (var lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            datos.load(lector);
        }
        int puerto;
        try {
            puerto = Integer.parseInt(datos.getProperty("puerto", "5432").strip());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("El puerto debe ser un número.");
        }
        if (puerto < 1 || puerto > 65535)
            throw new IllegalArgumentException("El puerto debe estar entre 1 y 65535.");
        String base = datos.getProperty("base", "inventario_ti").strip();
        String servidor = datos.getProperty("servidor", "localhost").strip();
        String usuario = datos.getProperty("usuario", "postgres").strip();
        if (base.isEmpty() || servidor.isEmpty() || usuario.isEmpty()) {
            throw new IllegalArgumentException(
                    "Completa servidor, base y usuario en configuracion.properties.");
        }
        return new Configuracion(servidor, puerto, base, usuario, datos.getProperty("clave", ""));
    }
}
