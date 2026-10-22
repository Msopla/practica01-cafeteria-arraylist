package ec.edu.ups.cafeteria;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class Main {
    private final BufferedReader entrada = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    private final RepositorioArchivos repositorio;
    private final ServicioCafeteria servicio;

    private Main(RepositorioArchivos repositorio, ServicioCafeteria servicio) {
        this.repositorio = repositorio;
        this.servicio = servicio;
    }

    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Uso: java ec.edu.ups.cafeteria.Main [carpeta-datos]");
            System.exit(1);
        }
        RepositorioArchivos repositorio = new RepositorioArchivos(new File(args.length == 1 ? args[0] : "datos"));
        try {
            new Main(repositorio, repositorio.cargar()).ejecutar();
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("No se pudo continuar: " + e.getMessage());
            System.exit(1);
        }
    }

    private void ejecutar() throws IOException {
        System.out.println("CAFETERÍA EL BUEN SABOR\nSistema de Gestión de Pedidos");
        try {
            while (true) {
                mostrarMenu();
                try {
                    String opcion = leer("Seleccione una opción: ");
                    switch (opcion) {
                        case "1": registrarProducto(); break;
                        case "2": listarProductos(); break;
                        case "3": reponerStock(); break;
                        case "4": crearPedido(); break;
                        case "5": reporte(); break;
                        case "6": guardar(); break;
                        case "0": if (guardar()) { System.out.println("Sesión finalizada."); return; } break;
                        default: System.out.println("Opción inválida. Intente otra vez.");
                    }
                } catch (Cancelacion e) {
                    System.out.println("Operación cancelada. No se realizaron cambios.");
                } catch (IllegalArgumentException e) {
                    System.out.println("No se realizó la operación: " + e.getMessage());
                }
            }
        } catch (EOFException e) {
            System.out.println("\nEntrada finalizada. Se guardarán las operaciones confirmadas.");
            if (!guardar()) throw new IOException("La entrada se cerró y no fue posible guardar los datos.");
        }
    }

    private void mostrarMenu() {
        System.out.println("\n1. Registrar producto\n2. Listar productos\n3. Reponer stock"
                + "\n4. Crear pedido\n5. Reporte de ventas por fecha\n6. Guardar información\n0. Guardar y salir");
        if (servicio.hayCambiosPendientes()) System.out.println("Hay cambios pendientes de guardar.");
    }

    private String leer(String mensaje) throws IOException {
        System.out.print(mensaje);
        String valor = entrada.readLine();
        if (valor == null) throw new EOFException();
        return valor.trim();
    }

    private String campo(String mensaje) throws IOException {
        String valor = leer(mensaje + " (o /cancelar): ");
        if (valor.equalsIgnoreCase("/cancelar")) throw new Cancelacion();
        return valor;
    }

    private int leerEntero(String mensaje, int minimo) throws IOException {
        while (true) {
            String valor = campo(mensaje);
            try {
                int numero = Integer.parseInt(valor);
                if (numero >= minimo) return numero;
            } catch (NumberFormatException e) { }
            System.out.println("Ingrese un entero entre " + minimo + " y " + Integer.MAX_VALUE + ".");
        }
    }

    private BigDecimal leerPrecio() throws IOException {
        while (true) {
            String valor = campo("Precio").replace(',', '.');
            try {
                if (!valor.matches("[0-9]+(\\.[0-9]{1,2})?")) throw new IllegalArgumentException();
                return Producto.validarPrecio(new BigDecimal(valor));
            } catch (IllegalArgumentException e) {
                System.out.println("Ingrese un precio positivo, con hasta dos decimales.");
            }
        }
    }

    private void registrarProducto() throws IOException {
        String codigo = campo("Código");
        String nombre = campo("Nombre");
        BigDecimal precio = leerPrecio();
        int stock = leerEntero("Stock inicial", 0);
        servicio.registrarProducto(new Producto(codigo, nombre, precio, stock));
        System.out.println("Producto registrado.");
    }

    private void listarProductos() {
        List<Producto> productos = servicio.listarProductos();
        if (productos.isEmpty()) { System.out.println("El catálogo está vacío."); return; }
        System.out.println("CÓDIGO | NOMBRE | PRECIO | STOCK");
        for (Producto p : productos) System.out.println(p.getCodigo() + " | " + p.getNombre()
                + " | $" + p.getPrecio().toPlainString() + " | " + p.getStock());
    }

    private void reponerStock() throws IOException {
        String codigo = campo("Código del producto");
        Producto producto = servicio.buscarProducto(codigo);
        System.out.println(producto.getNombre() + "; stock actual: " + producto.getStock());
        servicio.reponerStock(codigo, leerEntero("Unidades a reponer", 1));
        System.out.println("Stock actualizado: " + servicio.buscarProducto(codigo).getStock());
    }

    private void crearPedido() throws IOException {
        if (servicio.listarProductos().isEmpty()) {
            System.out.println("Registre productos antes de crear pedidos."); return;
        }
        Pedido pedido = servicio.crearPedido();
        try {
            while (true) {
                mostrarResumen(pedido);
                String opcion = campo("1. Agregar producto | 2. Confirmar pedido | 0. Cancelar");
                if (opcion.equals("0")) return;
                try {
                    if (opcion.equals("1")) {
                        String codigo = campo("Código");
                        Producto producto = servicio.buscarProducto(codigo);
                        System.out.println(producto.getNombre() + " | $" + producto.getPrecio()
                                + " | disponibles: " + producto.getStock());
                        servicio.agregarAlPedido(pedido, codigo, leerEntero("Cantidad", 1));
                    } else if (opcion.equals("2")) {
                        servicio.confirmarPedido(pedido);
                        System.out.println("Pedido confirmado #" + pedido.getId() + " | " + pedido.getFechaHora());
                        mostrarResumen(pedido);
                        return;
                    } else System.out.println("Opción inválida.");
                } catch (IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                }
            }
        } finally {
            if (!pedido.estaConfirmado()) {
                servicio.cancelarPedido(pedido);
                System.out.println("Borrador cancelado; stock sin cambios.");
            }
        }
    }

    private void mostrarResumen(Pedido pedido) {
        System.out.println("\nRESUMEN DEL PEDIDO");
        for (DetallePedido d : pedido.getDetalles()) System.out.println(d.getCodigoProducto() + " | "
                + d.getNombreProducto() + " | " + d.getCantidad() + " x $" + d.getPrecioUnitario()
                + " = $" + d.calcularSubtotal());
        System.out.println("Total: $" + pedido.calcularTotal());
    }

    private void reporte() throws IOException {
        while (true) {
            String valor = campo("Fecha AAAA-MM-DD (Enter para hoy)");
            try {
                System.out.println(servicio.reporteVentas(valor.isEmpty() ? LocalDate.now() : LocalDate.parse(valor)));
                return;
            } catch (DateTimeParseException e) {
                System.out.println("Fecha inválida. Ejemplo: 2026-10-21.");
            }
        }
    }

    private boolean guardar() {
        try {
            repositorio.guardar(servicio.listarProductos(), servicio.getPedidos());
            servicio.marcarGuardado();
            System.out.println("Información guardada correctamente.");
            return true;
        } catch (IOException e) {
            System.out.println(e.getMessage() + " Los datos permanecen en memoria; puede reintentar.");
            return false;
        }
    }

    private static final class Cancelacion extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
