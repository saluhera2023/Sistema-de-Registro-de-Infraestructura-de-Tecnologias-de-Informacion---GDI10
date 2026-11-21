package inventario.servicio;

import inventario.modelo.Equipo;
import inventario.modelo.Registro;

// Reglas de entrada que se aplican aunque la operación venga de otra pantalla.
final class Validaciones {
    private Validaciones() {}

    static void equipo(Equipo equipo) {
        requerido(equipo.codigo(), "Código de inventario", 20);
        texto(equipo.nombre(), "Nombre", 80);
        texto(equipo.modelo(), "Modelo", 80);
        texto(equipo.serie(), "Número de serie", 80);
        if (equipo.tipo() == null || equipo.estado() == null) {
            throw new IllegalArgumentException("Selecciona el tipo y el estado del equipo.");
        }
        if (equipo.compra() != null
                && equipo.garantia() != null
                && equipo.garantia().isBefore(equipo.compra())) {
            throw new IllegalArgumentException("La garantía no puede terminar antes de la compra.");
        }
    }

    static void catalogo(String entidad, Registro registro) {
        entidad(entidad);
        requerido(registro.nombre(), "Nombre", entidad.equals("software") ? 120 : 100);
        texto(registro.tipo(), "Tipo", 80);
        texto(registro.version(), "Versión", 60);
    }

    static void entidad(String entidad) {
        if (!"software".equals(entidad) && !"componente".equals(entidad)) {
            throw new IllegalArgumentException("El catálogo debe ser software o componente.");
        }
    }

    static void texto(String valor, String campo, int limite) {
        if (valor != null && valor.strip().codePointCount(0, valor.strip().length()) > limite) {
            throw new IllegalArgumentException(campo + " admite hasta " + limite + " caracteres.");
        }
    }

    private static void requerido(String valor, String campo, int limite) {
        if (valor == null || valor.isBlank())
            throw new IllegalArgumentException(campo + " es obligatorio.");
        texto(valor, campo, limite);
    }
}
