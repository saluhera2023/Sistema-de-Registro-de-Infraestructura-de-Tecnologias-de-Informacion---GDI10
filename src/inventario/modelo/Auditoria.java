package inventario.modelo;


// Valores anteriores y nuevos de un cambio registrado.
public record Auditoria(
        long id,
        String fecha,
        String usuario,
        String tabla,
        String operacion,
        String clave,
        String anterior,
        String nuevo) {}
