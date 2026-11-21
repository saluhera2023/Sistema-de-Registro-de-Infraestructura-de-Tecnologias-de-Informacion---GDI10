package inventario.util;

import java.sql.SQLException;

// Traduce los fallos técnicos a mensajes breves.
public final class Errores {
    private Errores() {}

    public static String describir(Throwable error) {
        if (error instanceof SQLException sql && sql.getSQLState() != null) {
            return switch (sql.getSQLState()) {
                case "28P01", "28000" -> "PostgreSQL rechazó el usuario o la contraseña.";
                case "3D000" -> "La base indicada no existe en el servidor.";
                case "23505" -> "El código o la asociación ya existe. Actualiza el listado.";
                case "23503" -> "Una referencia ya no existe. Actualiza el formulario.";
                case "23502" -> "Completa los campos obligatorios.";
                case "22001" -> "Un texto supera la longitud permitida.";
                case "22P02" -> "La dirección IP o un identificador tiene un formato inválido.";
                case "40001" ->
                        "El registro cambió desde que lo abriste. Actualiza el listado y vuelve a"
                                + " editar.";
                case "23514", "22023" -> primeraLinea(sql.getMessage());
                case "42501" -> "El usuario carece de permisos para esta operación.";
                case "42883", "42703", "42P01" ->
                        "Prepara la base con sql/inventario.sql antes de abrir el sistema.";
                default ->
                        sql.getSQLState().startsWith("08")
                                ? "No se pudo acceder a PostgreSQL. Revisa que esté iniciado y"
                                        + " comprueba configuracion.properties."
                                : "Error de PostgreSQL (SQLSTATE " + sql.getSQLState() + ").";
            };
        }
        if (error instanceof IllegalArgumentException) return error.getMessage();
        return "No se pudo completar la operación. Actualiza la consulta.";
    }

    private static String primeraLinea(String texto) {
        return texto.split("\\R", 2)[0].replaceFirst("^ERROR: ", "");
    }
}
