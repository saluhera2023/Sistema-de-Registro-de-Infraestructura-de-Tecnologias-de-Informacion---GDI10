package inventario.modelo;

import java.time.LocalDate;

// Identificación, ubicación y estado de un equipo.
public record Equipo(
        Integer id,
        int revision,
        boolean activo,
        String codigo,
        String nombre,
        String modelo,
        String serie,
        Integer tipo,
        Integer estado,
        Integer oficina,
        Integer area,
        Integer empleado,
        Integer marca,
        Integer principal,
        String ip,
        LocalDate compra,
        LocalDate garantia,
        String tipoTexto,
        String estadoTexto,
        String oficinaTexto,
        String empleadoTexto) {}
