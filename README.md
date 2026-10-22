# Práctica 01: Cafetería El Buen Sabor

Sistema de consola en Java para administrar productos, inventario, pedidos y ventas. Utiliza programación orientada a objetos, `ArrayList` y persistencia en `productos.txt` y `pedidos.txt` mediante `File` y flujos de texto UTF-8.

- **Estudiante:** Matias Fabian Martinez Espinoza
- **Universidad:** Universidad Politécnica Salesiana
- **Carrera:** Computación
- **Asignatura:** Programación Aplicada
- **Docente:** Ing. Daniel Diaz
- **Período:** PA69
- **Avance:** implementación del diseño de la práctica 01
- **Fecha:** 21 de octubre de 2026

## Iniciar en Windows

1. Extraer completamente el `.rar` y abrir la carpeta del proyecto.
2. Abrir una terminal dentro de esa carpeta.
3. Ejecutar:

```powershell
.\ejecutar.cmd
```

Se requiere un **JDK 8 o posterior**, no solamente un JRE. El script busca `javac` en `JAVA_HOME`, `PATH`, instalaciones habituales de Java y el runtime completo de Eclipse. No modifica la configuración de Windows. Compila las fuentes y ejecuta el programa. No requiere Maven ni bibliotecas externas.

Para comprobar el código:

```powershell
.\probar.cmd
```

Para compilar sin iniciar:

```powershell
.\compilar.cmd
java -Dfile.encoding=UTF-8 -jar build\cafeteria.jar
```

Si no se detecta el JDK, establecer `JAVA_HOME` en la terminal a la carpeta de su JDK y volver a ejecutar. Ejemplo de estructura: `JAVA_HOME=C:\Program Files\Java\jdk-17` (debe corresponder a una instalación real).

## Eclipse, NetBeans o IntelliJ IDEA

El proyecto incluye `pom.xml` para importarlo como proyecto Maven. Seleccionar un JDK instalado y ejecutar la clase **`ec.edu.ups.cafeteria.Main`**. En Eclipse: `File > Import > Maven > Existing Maven Projects`, seleccionar esta carpeta y ejecutar `Main.java` como aplicación Java.

También puede importarse como proyecto Java convencional: `src/main/java` contiene el programa y `src/test/java` las pruebas. La carpeta de trabajo de la ejecución debe ser la raíz del proyecto.

Si se dispone de Maven:

```text
mvn test
mvn package
java -Dfile.encoding=UTF-8 -jar target/cafeteria-arraylist-1.0.0.jar
```

El ciclo `test` ejecuta el verificador de esta práctica, sin JUnit. Maven puede descargar sus plugins durante el primer uso. La validación local de esta entrega se realizó con los scripts del JDK, no con Maven.

En Linux/macOS, con JDK en `PATH`:

```sh
sh ejecutar.sh
sh ejecutar.sh --test
```

## Menú

```text
1. Registrar producto
2. Listar productos
3. Reponer stock
4. Crear pedido
5. Reporte de ventas por fecha
6. Guardar información
0. Guardar y salir
```

- El registro solicita código, nombre, precio positivo y stock entero no negativo. Los códigos se normalizan a mayúsculas y no se repiten.
- El precio acepta punto o coma decimal, con hasta dos decimales. Los cálculos usan `BigDecimal`.
- En un pedido, elegir `1` para agregar una línea, `2` para confirmar o `0` para cancelar. Si se repite un producto, se acumula su cantidad.
- El stock cambia únicamente al confirmar todo el pedido. Un rechazo, un pedido vacío o una cancelación no descuentan unidades.
- El reporte solicita una fecha `AAAA-MM-DD`; Enter consulta el día actual. Muestra número de pedidos, importe total y demanda ordenada por unidades.
- Escribir `/cancelar` en los formularios regresa al menú. En un pedido, descarta todo el borrador.
- Usar `6` para guardar durante la sesión o `0` para guardar y salir. Si el guardado falla, el menú permanece disponible y los datos siguen en memoria.
- Si se cierra la entrada estándar, se descarta el borrador y se intenta guardar lo confirmado. Si ese guardado falla, el proceso termina con código de error.

## Archivos

La primera ejecución empieza vacía. Al guardar se crea la carpeta `datos/`, excluida de Git para no publicar ventas de uso local. Para usar otra carpeta:

```powershell
.\ejecutar.cmd "C:\ruta\mis-datos"
```

Los archivos son UTF-8 con separador `;`, cabecera obligatoria y punto decimal. Los textos no admiten `;` ni caracteres de control.

`productos.txt`:

```text
codigo;nombre;precio;stock
P001;Café americano;1.50;10
P002;Sándwich;2.25;5
```

`pedidos.txt` almacena una línea por detalle. Las líneas de un mismo ID tienen la misma fecha/hora:

```text
idPedido;fechaHora;codigoProducto;nombreProducto;cantidad;precioUnitario
1;2026-10-21T10:30:00;P001;Café americano;2;1.50
1;2026-10-21T10:30:00;P002;Sándwich;1;2.25
```

Estos registros de pedidos son ilustrativos. El catálogo real debe contener el stock restante después de sus ventas; al cargar no se vuelve a descontar. El nombre y precio de cada detalle conservan el valor histórico de la venta.

Para empezar con un catálogo de demostración, copiar **ambos** archivos de `ejemplos/` a una carpeta `datos/` nueva. El ejemplo incluye dos productos y ningún pedido. No reemplazar archivos de una sesión con ventas que se quieran conservar.

Si existe solo uno de los archivos, la cabecera es incorrecta, hay duplicados, fechas inválidas o productos desconocidos en los pedidos, el inicio se detiene e informa el error. No se sobrescriben los archivos al fallar la carga.

El guardado prepara archivos temporales y copias de respaldo; ante una excepción de escritura intenta restaurar la pareja anterior. Si esa restauración también falla, conserva los `.bak` e informa el problema. Esta práctica es de uso local y único usuario: no garantiza transacciones ante cortes eléctricos ni soporta dos instancias escribiendo simultáneamente. Cerrar con `0` y guardar periódicamente.

## Organización del código

```text
src/main/java/ec/edu/ups/cafeteria/
    Main.java                 Menú, lectura y mensajes
    Producto.java             Datos del catálogo y validaciones
    DetallePedido.java        Copia inmutable de una línea de venta
    Pedido.java               Borrador, detalles, total y confirmación
    ServicioCafeteria.java     Inventario, pedidos y reportes
    RepositorioArchivos.java   Lectura y escritura con validación
src/test/java/ec/edu/ups/cafeteria/
    PruebasCafeteria.java      21 casos automáticos sin dependencias
scripts/
    localizar-java.cmd        Detección del JDK en Windows
ejemplos/                     Datos ficticios para comenzar
docs/                         Trazabilidad y resultados observados
```

## Relación con el diseño anterior

Se conservan las seis clases principales, el menú y los formatos de archivo propuestos. Se concretaron estos detalles:

- `DetallePedido` es inmutable: consolidar unidades reemplaza la línea por otra con la cantidad acumulada.
- `Pedido` impide cambiar un pedido confirmado o cancelado. El servicio controla qué borradores están activos.
- Las consultas devuelven colecciones protegidas y copias de productos para evitar cambios externos del inventario.
- `Clock` permite probar fechas de manera reproducible; en uso normal se utiliza el reloj local del equipo.
- El repositorio agrega validación de líneas y restauración de respaldo ante errores de escritura.

## Verificación

Se compilaron las fuentes para Java 8 con el JDK disponible de Eclipse. Los **21 casos automáticos** pasaron. También se verificaron **7 escenarios de consola** sobre Java `1.8.0_481`, incluyendo reinicio y conservación del stock, cancelación, EOF y rechazo de datos corruptos.

Los resultados y la relación con RF/RNF están en [docs/VALIDACION.md](docs/VALIDACION.md). El código entregado no incluye impuestos, descuentos, devoluciones, facturación electrónica, base de datos ni una interfaz gráfica.
