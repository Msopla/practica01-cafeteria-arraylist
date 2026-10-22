package ec.edu.ups.cafeteria;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ServicioCafeteria {
    private final ArrayList<Producto> productos = new ArrayList<>();
    private final ArrayList<Pedido> pedidos = new ArrayList<>();
    private final ArrayList<Pedido> borradores = new ArrayList<>();
    private final Clock reloj;
    private boolean cambiosPendientes;

    public ServicioCafeteria(List<Producto> productos, List<Pedido> pedidos) {
        this(productos, pedidos, Clock.systemDefaultZone());
    }

    ServicioCafeteria(List<Producto> productos, List<Pedido> pedidos, Clock reloj) {
        this.reloj = reloj;
        for (Producto p : productos) registrarProducto(p);
        for (Pedido p : pedidos) {
            if (!p.estaConfirmado()) throw new IllegalArgumentException("Pedido sin confirmar en los datos.");
            for (Pedido anterior : this.pedidos)
                if (anterior.getId() == p.getId()) throw new IllegalArgumentException("ID de pedido duplicado.");
            for (DetallePedido d : p.getDetalles()) obtenerProducto(d.getCodigoProducto());
            this.pedidos.add(p);
        }
        cambiosPendientes = false;
    }

    public void registrarProducto(Producto producto) {
        for (Producto p : productos)
            if (p.getCodigo().equals(producto.getCodigo()))
                throw new IllegalArgumentException("Código ya registrado: " + p.getCodigo());
        productos.add(producto.copiar());
        cambiosPendientes = true;
    }

    private Producto obtenerProducto(String codigo) {
        String normalizado = Producto.normalizarCodigo(codigo);
        for (Producto p : productos) if (p.getCodigo().equals(normalizado)) return p;
        throw new IllegalArgumentException("Producto inexistente: " + normalizado);
    }

    public Producto buscarProducto(String codigo) { return obtenerProducto(codigo).copiar(); }

    public List<Producto> listarProductos() {
        ArrayList<Producto> copia = new ArrayList<>();
        for (Producto p : productos) copia.add(p.copiar());
        return Collections.unmodifiableList(copia);
    }

    public void reponerStock(String codigo, int cantidad) {
        obtenerProducto(codigo).reponer(cantidad);
        cambiosPendientes = true;
    }

    public Pedido crearPedido() {
        Pedido pedido = new Pedido();
        borradores.add(pedido);
        return pedido;
    }

    private void exigirBorradorPropio(Pedido pedido) {
        if (pedido == null || !borradores.contains(pedido))
            throw new IllegalArgumentException("El pedido no es un borrador activo de este sistema.");
        pedido.exigirBorrador();
    }

    public void agregarAlPedido(Pedido pedido, String codigo, int cantidad) {
        exigirBorradorPropio(pedido);
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser un entero positivo.");
        Producto producto = obtenerProducto(codigo);
        long acumulada = cantidad;
        for (DetallePedido d : pedido.getDetalles())
            if (d.getCodigoProducto().equals(producto.getCodigo())) acumulada += d.getCantidad();
        if (acumulada > producto.getStock())
            throw new IllegalArgumentException("Stock insuficiente: disponibles " + producto.getStock()
                    + "; solicitados en total " + acumulada + ".");
        pedido.agregarDetalle(new DetallePedido(producto.getCodigo(), producto.getNombre(),
                producto.getPrecio(), cantidad));
    }

    public void confirmarPedido(Pedido pedido) {
        exigirBorradorPropio(pedido);
        if (pedido.estaVacio()) throw new IllegalArgumentException("No se puede confirmar un pedido vacío.");
        for (DetallePedido d : pedido.getDetalles())
            if (d.getCantidad() > obtenerProducto(d.getCodigoProducto()).getStock())
                throw new IllegalArgumentException("Stock insuficiente para " + d.getCodigoProducto());
        int ultimoId = 0;
        for (Pedido p : pedidos) ultimoId = Math.max(ultimoId, p.getId());
        if (ultimoId == Integer.MAX_VALUE) throw new IllegalArgumentException("Se agotaron los ID de pedidos.");
        LocalDateTime fecha = LocalDateTime.now(reloj).withNano(0);
        pedido.confirmar(ultimoId + 1, fecha);
        for (DetallePedido d : pedido.getDetalles()) obtenerProducto(d.getCodigoProducto()).descontar(d.getCantidad());
        pedidos.add(pedido);
        borradores.remove(pedido);
        cambiosPendientes = true;
    }

    public void cancelarPedido(Pedido pedido) {
        exigirBorradorPropio(pedido);
        pedido.cancelar();
        borradores.remove(pedido);
    }

    public List<Pedido> getPedidos() { return Collections.unmodifiableList(new ArrayList<>(pedidos)); }
    public boolean hayCambiosPendientes() { return cambiosPendientes; }
    public void marcarGuardado() { cambiosPendientes = false; }

    public String reporteVentas(LocalDate fecha) {
        if (fecha == null) throw new IllegalArgumentException("Fecha requerida.");
        int cantidadPedidos = 0;
        BigDecimal total = new BigDecimal("0.00");
        ArrayList<VentaProducto> ranking = new ArrayList<>();
        for (Pedido pedido : pedidos) {
            if (!pedido.getFechaHora().toLocalDate().equals(fecha)) continue;
            cantidadPedidos++;
            total = total.add(pedido.calcularTotal());
            for (DetallePedido detalle : pedido.getDetalles()) {
                VentaProducto venta = null;
                for (VentaProducto candidata : ranking)
                    if (candidata.codigo.equals(detalle.getCodigoProducto())) { venta = candidata; break; }
                if (venta == null) {
                    venta = new VentaProducto(detalle.getCodigoProducto(), detalle.getNombreProducto());
                    ranking.add(venta);
                }
                venta.unidades += detalle.getCantidad();
                venta.importe = venta.importe.add(detalle.calcularSubtotal());
            }
        }
        ranking.sort((a, b) -> {
            int comparacion = Long.compare(b.unidades, a.unidades);
            return comparacion != 0 ? comparacion : a.codigo.compareTo(b.codigo);
        });
        StringBuilder salida = new StringBuilder("VENTAS DEL ").append(fecha)
                .append("\nPedidos: ").append(cantidadPedidos).append("\nTotal vendido: $")
                .append(total.toPlainString()).append("\n");
        if (ranking.isEmpty()) salida.append("No hay ventas para esta fecha.\n");
        else {
            salida.append("Demanda por producto (unidades descendentes):\n");
            for (VentaProducto venta : ranking) salida.append(venta.codigo).append(" | ")
                    .append(venta.nombre).append(" | unidades: ").append(venta.unidades)
                    .append(" | importe: $").append(venta.importe.toPlainString()).append("\n");
        }
        return salida.toString();
    }

    private static final class VentaProducto {
        private final String codigo;
        private final String nombre;
        private long unidades;
        private BigDecimal importe = new BigDecimal("0.00");
        private VentaProducto(String codigo, String nombre) { this.codigo = codigo; this.nombre = nombre; }
    }
}
