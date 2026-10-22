package ec.edu.ups.cafeteria;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/** Pruebas automáticas sin librerías externas. Fallar una comprobación termina con error. */
public final class PruebasCafeteria {
    private static int correctas;
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-21T15:30:00Z"), ZoneId.of("America/Guayaquil"));
    private static final String CP = "codigo;nombre;precio;stock\n";
    private static final String CD = "idPedido;fechaHora;codigoProducto;nombreProducto;cantidad;precioUnitario\n";

    private interface Accion { void ejecutar() throws Exception; }

    private static void caso(String nombre, Accion accion) throws Exception {
        accion.ejecutar(); correctas++; System.out.println("OK " + nombre);
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }

    private static void falla(Class<? extends Throwable> tipo, Accion accion) throws Exception {
        try { accion.ejecutar(); }
        catch (Throwable error) {
            if (tipo.isInstance(error)) return;
            throw new AssertionError("Excepción inesperada: " + error, error);
        }
        throw new AssertionError("Se esperaba " + tipo.getSimpleName());
    }

    private static ServicioCafeteria servicio() {
        return new ServicioCafeteria(Arrays.asList(
                new Producto("P001", "Café americano", new BigDecimal("1.50"), 10),
                new Producto("P002", "Sándwich", new BigDecimal("2.25"), 5)), Collections.emptyList(), RELOJ);
    }

    private static Pedido comprar(ServicioCafeteria s, String codigo, int cantidad) {
        Pedido p = s.crearPedido(); s.agregarAlPedido(p, codigo, cantidad); s.confirmarPedido(p); return p;
    }

    private static Path carpeta() throws IOException { return Files.createTempDirectory("cafeteria-test-"); }

    private static void escribir(Path carpeta, String productos, String pedidos) throws IOException {
        Files.write(carpeta.resolve("productos.txt"), productos.getBytes(StandardCharsets.UTF_8));
        Files.write(carpeta.resolve("pedidos.txt"), pedidos.getBytes(StandardCharsets.UTF_8));
    }

    private static void borrar(Path ruta) throws IOException {
        File[] hijos = ruta.toFile().listFiles();
        if (hijos != null) for (File hijo : hijos) borrar(hijo.toPath());
        Files.deleteIfExists(ruta);
    }

    public static void main(String[] args) throws Exception {
        caso("RF-01 registro y normalización de código", () -> {
            ServicioCafeteria s = servicio();
            s.registrarProducto(new Producto(" p003 ", "Agua", new BigDecimal("0.50"), 0));
            verificar(s.buscarProducto("p003").getStock() == 0 && s.listarProductos().size() == 3, "Registro incorrecto");
        });
        caso("RF-01 rechazar duplicados y datos inválidos", () -> {
            ServicioCafeteria s = servicio();
            falla(IllegalArgumentException.class, () -> s.registrarProducto(new Producto("p001", "Otro", BigDecimal.ONE, 1)));
            falla(IllegalArgumentException.class, () -> new Producto("", "A", BigDecimal.ONE, 0));
            falla(IllegalArgumentException.class, () -> new Producto("P3", "A;B", BigDecimal.ONE, 0));
            falla(IllegalArgumentException.class, () -> new Producto("P3", "A\nB", BigDecimal.ONE, 0));
            falla(IllegalArgumentException.class, () -> new Producto("P3", "A", BigDecimal.ZERO, 0));
            falla(IllegalArgumentException.class, () -> new Producto("P3", "A", new BigDecimal("1.001"), 0));
            falla(IllegalArgumentException.class, () -> new Producto("P3", "A", BigDecimal.ONE, -1));
            verificar(s.listarProductos().size() == 2, "Catálogo alterado por rechazo");
        });
        caso("RF-02 catálogo vacío y consulta defensiva", () -> {
            ServicioCafeteria vacio = new ServicioCafeteria(Collections.emptyList(), Collections.emptyList());
            verificar(vacio.listarProductos().isEmpty(), "Catálogo no vacío");
            ServicioCafeteria s = servicio(); s.buscarProducto("P001").reponer(10);
            verificar(s.buscarProducto("P001").getStock() == 10, "Referencia interna expuesta");
            falla(UnsupportedOperationException.class, () -> s.listarProductos().clear());
        });
        caso("RF-03 reposición válida y rechazos sin cambios", () -> {
            ServicioCafeteria s = servicio(); s.reponerStock("P002", 3);
            verificar(s.buscarProducto("P002").getStock() == 8, "Reposición incorrecta");
            falla(IllegalArgumentException.class, () -> s.reponerStock("P002", 0));
            falla(IllegalArgumentException.class, () -> s.reponerStock("NO", 1));
            falla(IllegalArgumentException.class, () -> s.reponerStock("P002", Integer.MAX_VALUE));
            verificar(s.buscarProducto("P002").getStock() == 8, "Reposición inválida cambió stock");
        });
        caso("RF-04 consolidación y stock acumulado", () -> {
            ServicioCafeteria s = servicio(); Pedido p = s.crearPedido();
            s.agregarAlPedido(p, "P001", 2); s.agregarAlPedido(p, "P001", 3);
            verificar(p.getDetalles().size() == 1 && p.getDetalles().get(0).getCantidad() == 5, "No se consolidó");
            falla(IllegalArgumentException.class, () -> s.agregarAlPedido(p, "P001", 6));
            falla(IllegalArgumentException.class, () -> s.agregarAlPedido(p, "NO", 1));
            falla(IllegalArgumentException.class, () -> s.agregarAlPedido(p, "P002", -1));
            verificar(s.buscarProducto("P001").getStock() == 10, "Un borrador cambió stock");
        });
        caso("RF-05 importes exactos de varios productos", () -> {
            ServicioCafeteria s = servicio(); Pedido p = s.crearPedido();
            s.agregarAlPedido(p, "P001", 2); s.agregarAlPedido(p, "P002", 1);
            verificar(p.calcularTotal().equals(new BigDecimal("5.25")), "Total no es 5.25");
            verificar(new DetallePedido("X", "X", new BigDecimal("0.10"), 3).calcularSubtotal().equals(new BigDecimal("0.30")), "Error decimal");
        });
        caso("RF-06 confirmación, fecha e inventario", () -> {
            ServicioCafeteria s = servicio(); Pedido p = comprar(s, "P001", 2);
            verificar(p.getId() == 1 && p.getFechaHora().toString().equals("2026-10-21T10:30"), "ID o fecha incorrectos");
            verificar(s.buscarProducto("P001").getStock() == 8 && s.getPedidos().size() == 1, "Venta no registrada");
            falla(IllegalArgumentException.class, () -> s.confirmarPedido(p));
            falla(IllegalArgumentException.class, () -> s.agregarAlPedido(p, "P001", 1));
            verificar(s.buscarProducto("P001").getStock() == 8, "Doble descuento");
        });
        caso("RF-06 stock justo, agotado y pedido vacío", () -> {
            ServicioCafeteria s = servicio(); comprar(s, "P002", 5);
            Pedido p = s.crearPedido();
            falla(IllegalArgumentException.class, () -> s.agregarAlPedido(p, "P002", 1));
            falla(IllegalArgumentException.class, () -> s.confirmarPedido(p));
            verificar(s.buscarProducto("P002").getStock() == 0, "Stock negativo");
        });
        caso("RF-06 revalidación completa sin descuento parcial", () -> {
            ServicioCafeteria s = servicio(); Pedido pendiente = s.crearPedido();
            s.agregarAlPedido(pendiente, "P001", 2); s.agregarAlPedido(pendiente, "P002", 5);
            comprar(s, "P002", 1);
            falla(IllegalArgumentException.class, () -> s.confirmarPedido(pendiente));
            verificar(s.buscarProducto("P001").getStock() == 10 && s.buscarProducto("P002").getStock() == 4, "Descuento parcial");
        });
        caso("RF-07 cancelación y borrador ajeno", () -> {
            ServicioCafeteria s = servicio(); Pedido p = s.crearPedido();
            s.agregarAlPedido(p, "P001", 2); s.cancelarPedido(p);
            falla(IllegalArgumentException.class, () -> s.confirmarPedido(p));
            falla(IllegalArgumentException.class, () -> s.confirmarPedido(servicio().crearPedido()));
            verificar(s.getPedidos().isEmpty() && s.buscarProducto("P001").getStock() == 10, "Cancelación alteró ventas");
        });
        caso("RF-08 reporte por fecha y orden de demanda", () -> {
            ServicioCafeteria s = servicio(); comprar(s, "P002", 3); comprar(s, "P001", 2);
            String r = s.reporteVentas(LocalDate.of(2026, 10, 21));
            verificar(r.contains("Pedidos: 2") && r.contains("Total vendido: $9.75"), "Resumen incorrecto");
            verificar(r.indexOf("P002") < r.indexOf("P001"), "Ranking incorrecto");
            verificar(s.reporteVentas(LocalDate.of(2026, 10, 20)).contains("Pedidos: 0\nTotal vendido: $0.00"), "Filtro de fecha incorrecto");
        });
        caso("RF-08 desempate por código", () -> {
            ServicioCafeteria s = servicio(); comprar(s, "P002", 1); comprar(s, "P001", 1);
            String r = s.reporteVentas(LocalDate.of(2026, 10, 21));
            verificar(r.indexOf("P001") < r.indexOf("P002"), "Desempate incorrecto");
        });
        caso("RF-09/10 primer inicio, ida/vuelta UTF-8 y continuidad de ID", () -> {
            Path dir = carpeta();
            try {
                RepositorioArchivos repo = new RepositorioArchivos(dir.toFile());
                verificar(repo.cargar().listarProductos().isEmpty(), "Primer inicio incorrecto");
                ServicioCafeteria s = servicio(); comprar(s, "P001", 2);
                repo.guardar(s.listarProductos(), s.getPedidos());
                ServicioCafeteria cargado = repo.cargar();
                verificar(cargado.buscarProducto("P001").getNombre().equals("Café americano"), "Se perdió UTF-8");
                verificar(cargado.buscarProducto("P001").getStock() == 8, "Stock se descontó otra vez");
                verificar(cargado.getPedidos().get(0).calcularTotal().toPlainString().equals("3.00"), "Venta perdida");
                verificar(comprar(cargado, "P002", 1).getId() == 2, "ID no continuó");
                repo.guardar(cargado.listarProductos(), cargado.getPedidos());
                verificar(repo.cargar().getPedidos().size() == 2, "Segundo guardado incorrecto");
            } finally { borrar(dir); }
        });
        caso("RF-09 archivos sin ventas conservan cabeceras", () -> {
            Path dir = carpeta(); try {
                RepositorioArchivos repo = new RepositorioArchivos(dir.toFile());
                repo.guardar(Collections.emptyList(), Collections.emptyList());
                verificar(repo.cargar().getPedidos().isEmpty(), "No cargó datos vacíos");
                verificar(Files.readAllLines(dir.resolve("pedidos.txt"), StandardCharsets.UTF_8).size() == 1, "Cabecera faltante");
            } finally { borrar(dir); }
        });
        caso("RF-09 error de escritura mantiene memoria", () -> {
            Path archivo = Files.createTempFile("cafeteria-bloqueo-", ".txt");
            try {
                ServicioCafeteria s = servicio(); comprar(s, "P001", 1);
                falla(IOException.class, () -> new RepositorioArchivos(archivo.toFile()).guardar(s.listarProductos(), s.getPedidos()));
                verificar(s.hayCambiosPendientes() && s.getPedidos().size() == 1, "Datos perdidos por fallo");
            } finally { borrar(archivo); }
        });
        caso("RF-10 falta un archivo: no iniciar vacío", () -> {
            Path dir = carpeta(); try {
                Files.write(dir.resolve("productos.txt"), CP.getBytes(StandardCharsets.UTF_8));
                falla(IOException.class, () -> new RepositorioArchivos(dir.toFile()).cargar());
            } finally { borrar(dir); }
        });
        caso("RF-10 errores de productos informan línea y conservan original", () -> {
            Path dir = carpeta(); try {
                for (String fila : Arrays.asList("P1;A;1.00;-1", "P1;A;NaN;1", "P1;A;1.00;1\np1;B;2.00;2", "P1;A;1.00;1;EXTRA")) {
                    escribir(dir, CP + fila + "\n", CD);
                    byte[] antes = Files.readAllBytes(dir.resolve("productos.txt"));
                    try { new RepositorioArchivos(dir.toFile()).cargar(); throw new AssertionError("No rechazó archivo"); }
                    catch (IOException e) { verificar(e.getMessage().contains("productos.txt, línea"), "Diagnóstico incompleto"); }
                    verificar(Arrays.equals(antes, Files.readAllBytes(dir.resolve("productos.txt"))), "Se alteró el archivo");
                }
                escribir(dir, "cabecera-invalida\n", CD);
                falla(IOException.class, () -> new RepositorioArchivos(dir.toFile()).cargar());
            } finally { borrar(dir); }
        });
        caso("RF-10 pedidos malformados, duplicados o huérfanos", () -> {
            Path dir = carpeta(); try {
                String valido = "1;2026-10-21T10:30:00;P1;A;1;1.00";
                for (String fila : Arrays.asList("0;2026-10-21T10:30:00;P1;A;1;1.00", "1;fecha;P1;A;1;1.00",
                        "1;2026-10-21T10:30:00;P1;A;0;1.00", "1;2026-10-21T10:30:00;NO;A;1;1.00",
                        valido + "\n" + valido, valido + "\n1;2026-10-22T10:30:00;P2;B;1;2.00")) {
                    escribir(dir, CP + "P1;A;1.00;5\nP2;B;2.00;5\n", CD + fila + "\n");
                    falla(IOException.class, () -> new RepositorioArchivos(dir.toFile()).cargar());
                }
            } finally { borrar(dir); }
        });
        caso("RNF-03 precio histórico independiente del catálogo", () -> {
            Path dir = carpeta(); try {
                escribir(dir, CP + "P1;Nombre actual;9.00;5\n", CD + "1;2026-10-21T10:30:00;P1;Nombre anterior;2;1.50\n");
                Pedido p = new RepositorioArchivos(dir.toFile()).cargar().getPedidos().get(0);
                verificar(p.calcularTotal().equals(new BigDecimal("3.00")), "Cambió el precio histórico");
            } finally { borrar(dir); }
        });
        caso("RNF-02 pedidos y detalles confirmados protegidos", () -> {
            ServicioCafeteria s = servicio(); Pedido p = comprar(s, "P001", 1);
            falla(UnsupportedOperationException.class, () -> s.getPedidos().clear());
            falla(UnsupportedOperationException.class, () -> p.getDetalles().clear());
            falla(IllegalArgumentException.class, () -> p.agregarDetalle(new DetallePedido("P001", "A", BigDecimal.ONE, 1)));
        });
        caso("RNF-06 catálogo y reporte con 1.000 registros", () -> {
            ArrayList<Producto> productos = new ArrayList<>();
            for (int i = 0; i < 1000; i++) productos.add(new Producto(String.format("P%04d", i), "Producto " + i, BigDecimal.ONE, 2));
            ServicioCafeteria s = new ServicioCafeteria(productos, Collections.emptyList(), RELOJ);
            for (Producto p : productos) comprar(s, p.getCodigo(), 1);
            long inicio = System.nanoTime();
            verificar(s.listarProductos().size() == 1000, "Catálogo incompleto");
            verificar(s.reporteVentas(LocalDate.of(2026, 10, 21)).contains("Total vendido: $1000.00"), "Reporte incompleto");
            long milisegundos = (System.nanoTime() - inicio) / 1_000_000;
            verificar(milisegundos < 2000, "Tiempo mayor a 2 segundos: " + milisegundos);
            System.out.println("Tiempo catálogo + reporte (1000 registros): " + milisegundos + " ms");
        });
        System.out.println("RESULTADO: " + correctas + " casos correctos; 0 fallidos.");
    }
}
