# Validación y trazabilidad del avance de código

Fecha: 21 de octubre de 2026.

## Entorno y ejecución

- Compilador: OpenJDK de Eclipse, `javac 25.0.4.1`, compilación con `--release 8` y UTF-8.
- Pruebas de clases: `probar.cmd`, **21 casos correctos y cero fallidos**.
- Pruebas de consola: **7 escenarios correctos y cero fallidos**, ejecutados en procesos Java separados con `java version "1.8.0_481"`.
- Los casos de persistencia trabajan en directorios temporales y los eliminan al terminar; no usan ventas personales.
- La captura de resultados de clases está en `resultados-pruebas.txt`; la de escenarios de consola, en `resultados-consola.txt`.
- Los scripts de Windows se ejecutaron localmente. La configuración Maven y el script POSIX se incluyen como alternativas; no se ejecutaron en esta validación local.

## Trazabilidad

| Requisito | Diseño / implementación | Comprobación realizada |
|---|---|---|
| RF-01 Registrar productos | `Producto`, `ServicioCafeteria.registrarProducto` | Alta, normalización, código duplicado, texto vacío, separadores, precio y stock inválidos. |
| RF-02 Listar catálogo | `listarProductos`, menú 2 | Catálogo vacío y lleno, consulta sin acceso a la colección interna; visualización en consola. |
| RF-03 Reponer stock | `reponerStock`, `Producto.reponer` | Suma correcta, producto inexistente, cero, negativos y desbordamiento. |
| RF-04 Formar pedido | `crearPedido`, `agregarAlPedido` | Varios productos, consolidación de repetidos, validación del stock acumulado. |
| RF-05 Calcular importes | `calcularSubtotal`, `calcularTotal` | Total 5.25 para dos cafés y un sándwich; 0.10 × 3 = 0.30 exacto. |
| RF-06 Confirmar | `confirmarPedido` | ID/fecha, stock justo, agotados, pedido vacío, doble confirmación y revalidación sin cambios parciales. |
| RF-07 Cancelar | `cancelarPedido`, menú del borrador | No registra ventas ni descuenta stock; borrador cancelado no se puede confirmar. |
| RF-08 Reportes | `reporteVentas` | Fecha sin ventas, suma, número de pedidos, ranking y desempate por código. |
| RF-09 Guardar | `RepositorioArchivos.guardar` | Archivos UTF-8, datos vacíos, guardado repetido y error de ruta sin pérdida de memoria. |
| RF-10 Recuperar | `cargarProductos`, `cargarPedidos`, `cargar` | Primer inicio, reinicio, ID siguiente, faltantes, cabeceras/filas inválidas, duplicados, referencias inexistentes y precio histórico. |
| RF-11 Menú y salida | `Main.ejecutar`, `guardar` | Opción inválida, corrección de entradas, cancelación, salida normal y fin de entrada. |
| RNF-01 Usabilidad | Menú y formularios en español | Corrección de precio/stock inválidos y retorno mediante `/cancelar`. |
| RNF-02 Mantenibilidad | Seis clases con responsabilidades separadas | Protección de colecciones, copias de productos y cierre de pedidos confirmados. |
| RNF-03 Exactitud | `BigDecimal`, detalles históricos | Operaciones decimales exactas y conservación del precio de la venta. |
| RNF-04 Conservación | Archivos y guardado al salir | Reinicio conserva ventas y stock sin descontarlo dos veces; carga inválida conserva los bytes originales. |
| RNF-05 Portabilidad | Java 8, UTF-8, rutas configurables | Ejecución en Java 8; acentos conservados; no hay rutas personales en las fuentes. No se probó otro sistema operativo. |
| RNF-06 Respuesta | Consultas de catálogo y reporte | Consulta conjunta sobre 1.000 productos y 1.000 pedidos menor a 2 segundos en el equipo local; tiempo concreto en el registro. |

## Alcance de la comprobación

No se simularon cortes eléctricos ni fallos del disco durante el reemplazo de los dos archivos. El mecanismo de respaldo reduce el riesgo de un error ordinario de escritura, pero no constituye una transacción de base de datos. No se validó el uso simultáneo por varias instancias, que está fuera del alcance de la práctica.

Para repetir las pruebas de clases, ejecutar `probar.cmd`. Para reproducir manualmente el recorrido de consola: registrar P001 (1.50, stock 10) y P002 (2.25, stock 5); vender 2 de P001 y 1 de P002; comprobar total 5.25; guardar y salir; reiniciar; comprobar stocks 8 y 4 y un pedido en el reporte del día.
