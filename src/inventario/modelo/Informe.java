package inventario.modelo;

import java.util.List;

// Columnas y filas que devuelve un reporte.
public record Informe(List<String> columnas, List<List<String>> filas) {}
