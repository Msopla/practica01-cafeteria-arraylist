package ec.edu.ups.cafeteria;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class RepositorioArchivos {
    private static final String CABECERA_PRODUCTOS = "codigo;nombre;precio;stock";
    private static final String CABECERA_PEDIDOS =
            "idPedido;fechaHora;codigoProducto;nombreProducto;cantidad;precioUnitario";
    private final File directorio;
    private final File archivoProductos;
    private final File archivoPedidos;

    public RepositorioArchivos(File directorio) {
        this.directorio = directorio;
        archivoProductos = new File(directorio, "productos.txt");
        archivoPedidos = new File(directorio, "pedidos.txt");
    }

    private void verificarPareja() throws IOException {
        if (directorio.exists() && !directorio.isDirectory())
            throw new IOException("La ruta de datos no es una carpeta.");
        if (archivoProductos.exists() != archivoPedidos.exists())
            throw new IOException("Datos incompletos: deben existir productos.txt y pedidos.txt juntos.");
        if (archivoProductos.exists() && (!archivoProductos.isFile() || !archivoPedidos.isFile()))
            throw new IOException("Las rutas de datos deben ser archivos regulares.");
    }

    private BufferedReader lector(File archivo) throws IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(archivo),
                StandardCharsets.UTF_8.newDecoder()));
    }

    private BufferedWriter escritor(File archivo) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8));
    }

    private String[] campos(String linea, int cantidad) {
        String[] campos = linea.split(";", -1);
        if (campos.length != cantidad) throw new IllegalArgumentException("Número de campos incorrecto.");
        return campos;
    }

    private int entero(String texto) {
        if (!texto.matches("[0-9]+")) throw new IllegalArgumentException("Se esperaba un entero no negativo.");
        return Integer.parseInt(texto);
    }

    private BigDecimal precio(String texto) {
        if (!texto.matches("[0-9]+\\.[0-9]{2}"))
            throw new IllegalArgumentException("Precio inválido; use punto y dos decimales.");
        return Producto.validarPrecio(new BigDecimal(texto));
    }

    private IOException error(File archivo, int linea, Exception causa) {
        return new IOException(archivo.getName() + ", línea " + linea + ": " + causa.getMessage(), causa);
    }

    public ArrayList<Producto> cargarProductos() throws IOException {
        verificarPareja();
        ArrayList<Producto> productos = new ArrayList<>();
        if (!archivoProductos.exists()) return productos;
        int numero = 1;
        try (BufferedReader entrada = lector(archivoProductos)) {
            if (!CABECERA_PRODUCTOS.equals(entrada.readLine()))
                throw new IllegalArgumentException("Cabecera de productos inválida.");
            String linea;
            while ((linea = entrada.readLine()) != null) {
                numero++;
                String[] datos = campos(linea, 4);
                Producto producto = new Producto(datos[0], datos[1], precio(datos[2]), entero(datos[3]));
                for (Producto anterior : productos)
                    if (anterior.getCodigo().equals(producto.getCodigo()))
                        throw new IllegalArgumentException("Código de producto duplicado.");
                productos.add(producto);
            }
        } catch (IllegalArgumentException e) {
            throw error(archivoProductos, numero, e);
        }
        return productos;
    }

    public ArrayList<Pedido> cargarPedidos() throws IOException {
        verificarPareja();
        ArrayList<Pedido> pedidos = new ArrayList<>();
        if (!archivoPedidos.exists()) return pedidos;
        ArrayList<Producto> catalogo = cargarProductos();
        ArrayList<Integer> ids = new ArrayList<>();
        ArrayList<LocalDateTime> fechas = new ArrayList<>();
        int numero = 1;
        try (BufferedReader entrada = lector(archivoPedidos)) {
            if (!CABECERA_PEDIDOS.equals(entrada.readLine()))
                throw new IllegalArgumentException("Cabecera de pedidos inválida.");
            String linea;
            while ((linea = entrada.readLine()) != null) {
                numero++;
                String[] datos = campos(linea, 6);
                int id = entero(datos[0]);
                if (id <= 0) throw new IllegalArgumentException("ID de pedido inválido.");
                LocalDateTime fecha = LocalDateTime.parse(datos[1], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                DetallePedido detalle = new DetallePedido(datos[2], datos[3], precio(datos[5]), entero(datos[4]));
                boolean existe = false;
                for (Producto p : catalogo) if (p.getCodigo().equals(detalle.getCodigoProducto())) existe = true;
                if (!existe) throw new IllegalArgumentException("El producto del pedido no existe en el catálogo.");
                int indice = ids.indexOf(id);
                if (indice < 0) {
                    ids.add(id);
                    fechas.add(fecha);
                    pedidos.add(new Pedido());
                    indice = pedidos.size() - 1;
                } else if (!fechas.get(indice).equals(fecha)) {
                    throw new IllegalArgumentException("Fechas diferentes para el mismo pedido.");
                }
                Pedido pedido = pedidos.get(indice);
                for (DetallePedido anterior : pedido.getDetalles())
                    if (anterior.getCodigoProducto().equals(detalle.getCodigoProducto()))
                        throw new IllegalArgumentException("Producto repetido dentro del mismo pedido.");
                pedido.agregarDetalle(detalle);
            }
        } catch (RuntimeException e) {
            throw error(archivoPedidos, numero, e);
        }
        for (int i = 0; i < pedidos.size(); i++) pedidos.get(i).confirmar(ids.get(i), fechas.get(i));
        return pedidos;
    }

    public ServicioCafeteria cargar() throws IOException {
        return new ServicioCafeteria(cargarProductos(), cargarPedidos());
    }

    public void guardar(List<Producto> productos, List<Pedido> pedidos) throws IOException {
        verificarPareja();
        new ServicioCafeteria(productos, pedidos);
        if (!directorio.isDirectory() && !directorio.mkdirs())
            throw new IOException("No se pudo crear la carpeta de datos.");
        File tempProductos = null, tempPedidos = null, copiaProductos = null, copiaPedidos = null;
        boolean habiaArchivos = archivoProductos.exists();
        boolean reemplazoIniciado = false;
        boolean recuperacionFallida = false;
        try {
            tempProductos = File.createTempFile("productos-", ".tmp", directorio);
            tempPedidos = File.createTempFile("pedidos-", ".tmp", directorio);
            try (BufferedWriter salida = escritor(tempProductos)) {
                salida.write(CABECERA_PRODUCTOS); salida.newLine();
                for (Producto p : productos) {
                    salida.write(p.getCodigo() + ";" + p.getNombre() + ";"
                            + p.getPrecio().toPlainString() + ";" + p.getStock());
                    salida.newLine();
                }
            }
            try (BufferedWriter salida = escritor(tempPedidos)) {
                salida.write(CABECERA_PEDIDOS); salida.newLine();
                for (Pedido p : pedidos) for (DetallePedido d : p.getDetalles()) {
                    salida.write(p.getId() + ";" + p.getFechaHora().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            + ";" + d.getCodigoProducto() + ";" + d.getNombreProducto() + ";"
                            + d.getCantidad() + ";" + d.getPrecioUnitario().toPlainString());
                    salida.newLine();
                }
            }
            if (habiaArchivos) {
                copiaProductos = File.createTempFile("productos-", ".bak", directorio);
                copiaPedidos = File.createTempFile("pedidos-", ".bak", directorio);
                Files.copy(archivoProductos.toPath(), copiaProductos.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.copy(archivoPedidos.toPath(), copiaPedidos.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            reemplazoIniciado = true;
            Files.move(tempProductos.toPath(), archivoProductos.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.move(tempPedidos.toPath(), archivoPedidos.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            if (reemplazoIniciado) {
                try {
                    if (habiaArchivos) {
                        Files.copy(copiaProductos.toPath(), archivoProductos.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        Files.copy(copiaPedidos.toPath(), archivoPedidos.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        Files.deleteIfExists(archivoProductos.toPath());
                        Files.deleteIfExists(archivoPedidos.toPath());
                    }
                } catch (IOException recuperacion) {
                    recuperacionFallida = true;
                    e.addSuppressed(recuperacion);
                }
            }
            throw new IOException("No se pudo guardar. "
                    + (recuperacionFallida ? "Revise los archivos y conserve las copias .bak. " : "")
                    + e.getMessage(), e);
        } finally {
            borrarTemporal(tempProductos); borrarTemporal(tempPedidos);
            if (!recuperacionFallida) { borrarTemporal(copiaProductos); borrarTemporal(copiaPedidos); }
        }
    }

    private void borrarTemporal(File archivo) {
        if (archivo != null && archivo.exists() && !archivo.delete()) archivo.deleteOnExit();
    }
}
