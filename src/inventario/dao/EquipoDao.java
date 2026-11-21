package inventario.dao;

import static inventario.dao.Consultas.*;

import inventario.config.Conexion;
import inventario.modelo.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

// Consulta y guarda equipos, sus catálogos y su hoja de vida.
public final class EquipoDao {
    private final Consultas consultas;

    public EquipoDao(Conexion conexiones) {
        this.consultas = new Consultas(conexiones);
    }

    public List<Equipo> listar(String filtro, boolean inactivos) throws SQLException {
        return consultas.leer(
                "sp_listar_inventario",
                f ->
                        new Equipo(
                                f.getInt("id_equipo"),
                                f.getInt("revision"),
                                f.getBoolean("activo"),
                                f.getString("codigo_inventario"),
                                f.getString("nombre_equipo"),
                                f.getString("modelo"),
                                f.getString("numero_serie"),
                                entero(f, "id_tipo"),
                                entero(f, "id_estado"),
                                entero(f, "id_oficina"),
                                entero(f, "id_area"),
                                entero(f, "id_empleado"),
                                entero(f, "id_marca"),
                                entero(f, "id_equipo_principal"),
                                f.getString("ip"),
                                f.getObject("fecha_compra", LocalDate.class),
                                f.getObject("garantia", LocalDate.class),
                                f.getString("tipo"),
                                f.getString("estado"),
                                f.getString("nombre_oficina"),
                                f.getString("nombres_empleado")),
                filtro,
                inactivos);
    }

    public List<Catalogo> catalogos() throws SQLException {
        return consultas.leer(
                "sp_catalogos_equipo",
                f -> new Catalogo(f.getString("catalogo"), f.getInt("id"), f.getString("nombre")));
    }

    public int guardar(Equipo equipo) throws SQLException {
        return consultas
                .escribirConSalida(
                        "sp_guardar_equipo",
                        equipo.id(),
                        equipo.revision(),
                        equipo.codigo(),
                        equipo.nombre(),
                        equipo.modelo(),
                        equipo.serie(),
                        equipo.tipo(),
                        equipo.estado(),
                        equipo.oficina(),
                        equipo.area(),
                        equipo.empleado(),
                        equipo.marca(),
                        equipo.principal(),
                        equipo.ip(),
                        equipo.compra(),
                        equipo.garantia())
                .intValue();
    }

    public void actividad(Equipo equipo, boolean activo) throws SQLException {
        consultas.escribir("sp_actividad_equipo", equipo.id(), equipo.revision(), activo);
    }

    public Optional<Ficha> ficha(String codigo) throws SQLException {
        return consultas
                .leer(
                        "sp_hoja_vida_equipo",
                        f -> {
                            var datos = new LinkedHashMap<String, String>();
                            datos.put("Tipo", f.getString("tipo"));
                            datos.put(
                                    "Marca y modelo",
                                    texto(f.getString("nombre_marca"))
                                            + " "
                                            + texto(f.getString("modelo")));
                            datos.put("Número de serie", f.getString("numero_serie"));
                            datos.put("Dirección IP", f.getString("ip"));
                            datos.put("Estado", f.getString("estado"));
                            datos.put("Oficina", f.getString("nombre_oficina"));
                            datos.put("Área", f.getString("nombre_area"));
                            datos.put("Asignado a", f.getString("asignado_a"));
                            datos.put("Fecha de compra", f.getString("fecha_compra"));
                            datos.put("Garantía", f.getString("garantia"));
                            return new Ficha(
                                    f.getString("codigo_inventario"),
                                    f.getString("nombre_equipo"),
                                    datos,
                                    arreglo(f, "perifericos"),
                                    arreglo(f, "componentes"),
                                    arreglo(f, "software"));
                        },
                        codigo)
                .stream()
                .findFirst();
    }
}
