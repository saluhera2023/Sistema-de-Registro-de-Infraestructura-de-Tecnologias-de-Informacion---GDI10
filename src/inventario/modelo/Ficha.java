package inventario.modelo;

import java.util.List;
import java.util.Map;

// Datos completos para la hoja de vida.
public record Ficha(
        String codigo,
        String nombre,
        Map<String, String> datos,
        List<String> perifericos,
        List<String> componentes,
        List<String> software) {}
