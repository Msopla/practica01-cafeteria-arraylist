package ec.edu.ups.cafeteria;

import java.math.BigDecimal;

public final class DetallePedido {
    private final String codigoProducto;
    private final String nombreProducto;
    private final BigDecimal precioUnitario;
    private final int cantidad;

    public DetallePedido(String codigo, String nombre, BigDecimal precio, int cantidad) {
        codigoProducto = Producto.normalizarCodigo(codigo);
        nombreProducto = Producto.validarTexto(nombre, "Nombre");
        precioUnitario = Producto.validarPrecio(precio);
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser positiva.");
        this.cantidad = cantidad;
    }

    public BigDecimal calcularSubtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public String getCodigoProducto() { return codigoProducto; }
    public String getNombreProducto() { return nombreProducto; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public int getCantidad() { return cantidad; }
}
