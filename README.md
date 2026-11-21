# Sistema de Registro de Infraestructura de Tecnologías de la Información

Aplicativo de escritorio con Java Swing, JDBC y PostgreSQL, sin frameworks.
El código conserva sus módulos de modelos, vistas, servicios, DAO y configuración.

## Abrir con un solo comando

Extrae el paquete completo en una carpeta nueva y abre una terminal dentro de
`RegistroInfraestructuraTI`. Ejecuta:

```powershell
.\Abrir_Inventario.cmd
```

También puedes abrir `Abrir_Inventario.cmd` con doble clic.

La distribución completa para **Windows 10 u 11 de 64 bits** incluye Java,
PostgreSQL y el aplicativo compilado. La primera ejecución prepara automáticamente
la base, genera sus datos de acceso y carga 90 equipos de prueba. Después abre
la ventana del sistema. No tienes que editar contraseñas ni ejecutar el SQL a mano.

Mantén abierta la terminal mientras utilizas el sistema. Para terminar, cierra
la ventana con la X; el iniciador detiene su PostgreSQL.

## Registros guardados

La carpeta `datos/` se crea al iniciar. Contiene la base y sus datos de conexión.
Los siguientes inicios reutilizan esa carpeta y conservan los registros.
Cada copia extraída del paquete tiene su propia base y utiliza un puerto local
disponible, sin configurar el servicio PostgreSQL instalado en la computadora.

Para respaldar los registros, cierra primero el sistema y copia toda la carpeta
`datos/`. No borres esa carpeta para solucionar un error. Los registros de inicio
y de PostgreSQL se guardan allí para revisar un fallo.

## Código organizado

| Carpeta | Función |
| --- | --- |
| `src/inventario/App.java` | Punto de entrada. |
| `src/inventario/modelo/` | Datos que utilizan los módulos. |
| `src/inventario/vista/` | Una pantalla por módulo, navegación y controles comunes. |
| `src/inventario/servicio/` | Validaciones y coordinación de operaciones. |
| `src/inventario/dao/` | Acceso JDBC separado por módulo. |
| `src/inventario/config/` | Lectura de configuración, conexiones y preparación de tablas. |
| `src/inventario/util/` | Mensajes de error compartidos. |
| `sql/inventario.sql` | Tablas, datos de prueba, procedimientos, triggers y 15 reportes. |
| `lib/` | Controlador JDBC y su licencia. |
| `bin/` | Aplicativo compilado de la distribución completa. |
| `runtime/` | Java y PostgreSQL de la distribución completa, con sus dependencias y licencias. |

Cada pantalla mantiene sus propios campos y selección. La ventana principal
organiza la navegación. Las pantallas usan el servicio; el servicio utiliza los
DAO y estos ejecutan los procedimientos almacenados.

El sistema permite registrar, buscar, modificar, desactivar y reactivar equipos;
gestionar software y componentes; consultar hojas de vida, reportes e historial;
exportar CSV e imprimir los reportes.

El catálogo `componente` guarda `id_componente` y `nombre_componente`.
La relación `equipo_componente` utiliza `(id_equipo, id_componente)` y guarda
`detalle`. Así, un mismo componente puede tener especificaciones distintas
según el equipo. Las validaciones, los borrados lógicos y la auditoría se conservan.

## Subir el código fuente a GitHub

Puedes usar el ZIP de código fuente o subir el contenido del proyecto completo
respetando `.gitignore`. Se incluyen `src/`, `sql/`, `lib/`, los tres archivos de
inicio y compilación, este README y `.gitignore`.

`.gitignore` deja fuera `datos/`, `build/`, `bin/` y `runtime/`. Las contraseñas
generadas, las bases locales y las copias del motor no se suben al repositorio.
El código necesario para iniciar y compilar sí está incluido.

En una copia de código fuente sin `runtime/` ni `bin/`, el mismo comando
`Abrir_Inventario.cmd` busca un **JDK 17 o posterior** en `JAVA_HOME` o el `PATH`,
compila los módulos y utiliza los binarios de un **PostgreSQL 17 o posterior**
instalado. PostgreSQL se detecta en el `PATH` o en `Program Files/PostgreSQL`.
También prepara una base propia; no pide la contraseña del servidor instalado.

Si la computadora no tiene esas herramientas, usa la distribución completa,
que ya las incluye. El ZIP de código fuente es ligero porque no duplica el motor.

## Recompilar después de modificar el código

Para modificar y compilar se requiere un JDK 17 o posterior. El iniciador detecta
cambios en los archivos Java y recompila antes de abrir el sistema. También puedes
recompilar explícitamente desde la terminal:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Compilar.ps1
```

El código está comentado con explicaciones breves. Los archivos de `runtime/`
pertenecen a Java y PostgreSQL; deben conservarse completos en la distribución
portátil para que funcione en otra computadora.
