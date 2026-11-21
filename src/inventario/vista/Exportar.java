package inventario.vista;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.JTable;

// Guarda los reportes en CSV para abrirlos en una hoja de cálculo.
final class Exportar {
    private Exportar() {}

    // Usa las filas visibles para respetar el orden elegido en la tabla.
    static void csv(JTable tabla, Path archivo) throws IOException {
        try (var salida = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
            salida.write('\uFEFF');
            for (int columna = 0; columna < tabla.getColumnCount(); columna++) {
                if (columna > 0) salida.write(';');
                salida.write(celda(tabla.getColumnName(columna)));
            }
            salida.write("\r\n");
            for (int fila = 0; fila < tabla.getRowCount(); fila++) {
                for (int columna = 0; columna < tabla.getColumnCount(); columna++) {
                    if (columna > 0) salida.write(';');
                    salida.write(celda(tabla.getValueAt(fila, columna)));
                }
                salida.write("\r\n");
            }
        }
    }

    // Evita interpretar el contenido como una fórmula al abrirlo en Excel.
    static String celda(Object valor) {
        String texto = valor == null ? "" : valor.toString();
        if (texto.matches("(?s)^[\\s]*[=+@-].*")) texto = "'" + texto;
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }

    static String titulo(String nombre) {
        String texto = nombre.replace('_', ' ');
        return texto.isEmpty()
                ? texto
                : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
