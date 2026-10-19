package dev.pimon.listillo.producto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Un producto por su código de barras: lo que sabe OFF o lo que alguien escribió. */
@Entity
@Table(name = "productos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Producto {

    @Id
    @Column(length = 14)
    private String ean;

    @Column(length = 120)
    private String nombre;

    @Column(length = 80)
    private String marca;

    @Column(length = 40)
    private String cantidad;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(length = 500)
    private String categorias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrigenProducto origen;

    @Column(name = "consultado_en", nullable = false)
    private LocalDateTime consultadoEn;

    @Column(name = "nombrado_por", length = 36)
    private String nombradoPor;

    private Producto(String ean, OrigenProducto origen) {
        this.ean = ean;
        this.origen = origen;
        this.consultadoEn = LocalDateTime.now();
    }

    public static Producto deOpenFoodFacts(String ean, DatosProducto datos) {
        Producto p = new Producto(ean, OrigenProducto.OPEN_FOOD_FACTS);
        p.copiar(datos);
        return p;
    }

    public static Producto desconocido(String ean) {
        return new Producto(ean, OrigenProducto.DESCONOCIDO);
    }

    /** OFF ha respondido otra vez: se refresca con lo que diga ahora. */
    public void actualizarDeOpenFoodFacts(DatosProducto datos) {
        this.origen = OrigenProducto.OPEN_FOOD_FACTS;
        this.nombradoPor = null;
        this.consultadoEn = LocalDateTime.now();
        copiar(datos);
    }

    /** OFF sigue sin conocerlo: se apunta la fecha para no preguntar en un tiempo. */
    public void marcarConsultado() {
        this.consultadoEn = LocalDateTime.now();
    }

    public void nombrar(String nombre, String marca, String cantidad, String quien) {
        this.origen = OrigenProducto.USUARIO;
        this.nombre = nombre;
        this.marca = marca;
        this.cantidad = cantidad;
        this.nombradoPor = quien;
    }

    public static Producto nombrado(String ean, String nombre, String marca, String cantidad, String quien) {
        Producto p = new Producto(ean, OrigenProducto.USUARIO);
        p.nombrar(nombre, marca, cantidad, quien);
        return p;
    }

    private void copiar(DatosProducto datos) {
        this.nombre = datos.nombre();
        this.marca = datos.marca();
        this.cantidad = datos.cantidad();
        this.imagenUrl = datos.imagenUrl();
        this.categorias = datos.categorias();
    }
}
