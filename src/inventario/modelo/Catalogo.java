package inventario.modelo;


// Una opción de los desplegables del formulario.
public record Catalogo(String grupo, Integer id, String nombre) {
    @Override
    public String toString() {
        return nombre;
    }
}
