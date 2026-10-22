package ec.edu.ups.cafeteria;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class Producto {
    private final String codigo;
    private final String nombre;
    private final BigDecimal precio;
    private int stock;

    public Producto(String codigo, String nombre, BigDecimal precio, int stock) {
        this.codigo = normalizarCodigo(codigo);
        this.nombre = validarTexto(nombre, "Nombre");
        this.precio = validarPrecio(precio);
        if (stock < 0) throw new IllegalArgumentException("El stock no puede ser negativo.");
        this.stock = stock;
    }

    static String validarTexto(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty())
            throw new IllegalArgumentException(campo + " no puede estar vacío.");
        String texto = valor.trim();
        if (texto.indexOf(';') >= 0 || texto.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException(campo + " no admite ; ni caracteres de control.");
        return texto;
    }

    static String normalizarCodigo(String codigo) {
        return validarTexto(codigo, "Código").toUpperCase(Locale.ROOT);
    }

    static BigDecimal validarPrecio(BigDecimal precio) {
        if (precio == null || precio.signum() <= 0)
            throw new IllegalArgumentException("El precio debe ser mayor que cero.");
        try {
            return precio.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("El precio admite hasta dos decimales.");
        }
    }

    void reponer(int cantidad) {
        if (cantidad <= 0) throw new IllegalArgumentException("La reposición debe ser positiva.");
        try {
            stock = Math.addExact(stock, cantidad);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("La reposición excede el límite de unidades.");
        }
    }

    void descontar(int cantidad) {
        if (cantidad <= 0 || cantidad > stock)
            throw new IllegalArgumentException("Stock insuficiente o cantidad inválida.");
        stock -= cantidad;
    }

    Producto copiar() { return new Producto(codigo, nombre, precio, stock); }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public BigDecimal getPrecio() { return precio; }
    public int getStock() { return stock; }
}
