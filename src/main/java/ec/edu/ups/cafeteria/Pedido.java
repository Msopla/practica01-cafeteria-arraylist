package ec.edu.ups.cafeteria;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Pedido {
    private int id;
    private LocalDateTime fechaHora;
    private final ArrayList<DetallePedido> detalles = new ArrayList<>();
    private boolean cancelado;

    Pedido() { }

    void exigirBorrador() {
        if (estaConfirmado() || cancelado)
            throw new IllegalArgumentException("El pedido ya está confirmado o cancelado.");
    }

    void agregarDetalle(DetallePedido detalle) {
        exigirBorrador();
        for (int i = 0; i < detalles.size(); i++) {
            DetallePedido anterior = detalles.get(i);
            if (anterior.getCodigoProducto().equals(detalle.getCodigoProducto())) {
                int cantidad;
                try {
                    cantidad = Math.addExact(anterior.getCantidad(), detalle.getCantidad());
                } catch (ArithmeticException e) {
                    throw new IllegalArgumentException("Cantidad demasiado grande.");
                }
                detalles.set(i, new DetallePedido(anterior.getCodigoProducto(),
                        anterior.getNombreProducto(), anterior.getPrecioUnitario(), cantidad));
                return;
            }
        }
        detalles.add(detalle);
    }

    void confirmar(int id, LocalDateTime fechaHora) {
        exigirBorrador();
        if (id <= 0 || fechaHora == null || estaVacio())
            throw new IllegalArgumentException("Un pedido confirmado requiere ID, fecha y detalles.");
        this.id = id;
        this.fechaHora = fechaHora;
    }

    void cancelar() { exigirBorrador(); cancelado = true; }

    public BigDecimal calcularTotal() {
        BigDecimal total = new BigDecimal("0.00");
        for (DetallePedido detalle : detalles) total = total.add(detalle.calcularSubtotal());
        return total;
    }

    public List<DetallePedido> getDetalles() {
        return Collections.unmodifiableList(new ArrayList<>(detalles));
    }

    public int getId() { return id; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public boolean estaVacio() { return detalles.isEmpty(); }
    public boolean estaConfirmado() { return id > 0; }
}
