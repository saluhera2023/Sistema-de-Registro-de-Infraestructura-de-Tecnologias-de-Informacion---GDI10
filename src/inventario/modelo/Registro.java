package inventario.modelo;


// Un registro del catálogo de software o componentes.
public record Registro(
        String clave, String nombre, String tipo, String version, boolean activo, int revision) {
    @Override
    public String toString() {
        return nombre + (version == null || version.isBlank() ? "" : " " + version);
    }
}
