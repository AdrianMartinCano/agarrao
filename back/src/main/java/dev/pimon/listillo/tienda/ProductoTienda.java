package dev.pimon.listillo.tienda;

import dev.pimon.listillo.ticket.Cadena;
import dev.pimon.listillo.ticket.Unidad;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Un producto del catálogo web de una cadena, con su precio de hoy y su genérico. */
@Entity
@Table(name = "productos_tienda")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductoTienda {

    @EmbeddedId
    private Clave clave;

    @Column(nullable = false, length = 160)
    private String nombre;

    /** Para buscar: minúsculas, sin acentos (Textos.normalizar). */
    @Column(name = "nombre_normalizado", nullable = false, length = 160)
    private String nombreNormalizado;

    @Column(length = 80)
    private String formato;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal tamano;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private Unidad unidad;

    @Column(name = "precio_unidad", nullable = false, precision = 10, scale = 3)
    private BigDecimal precioUnidad;

    @Column(length = 120)
    private String seccion;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(length = 500)
    private String url;

    @Column(name = "generico_id")
    private UUID genericoId;

    @Column(name = "generico_manual", nullable = false)
    private boolean genericoManual;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    /** WEB (la web de la cadena) u OPEN_PRICES. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private dev.pimon.listillo.generico.FuentePrecio fuente = dev.pimon.listillo.generico.FuentePrecio.WEB;

    /** De qué día es el precio, si no es de hoy (los de Open Prices). */
    @Column(name = "fecha_precio")
    private java.time.LocalDate fechaPrecio;

    public ProductoTienda(Cadena cadena, String idExterno) {
        this.clave = new Clave(cadena, idExterno);
    }

    /** Lo que dice la web hoy. El genérico corregido a mano no se toca. */
    public void actualizar(ProductoWeb web) {
        actualizar(web, dev.pimon.listillo.generico.FuentePrecio.WEB, null);
    }

    /** Lo leído de una fuente; {@code fechaPrecio} null = de hoy. */
    public void actualizar(ProductoWeb web, dev.pimon.listillo.generico.FuentePrecio fuente, java.time.LocalDate fechaPrecio) {
        this.fuente = fuente;
        this.fechaPrecio = fechaPrecio;
        this.nombre = web.nombre();
        this.nombreNormalizado = dev.pimon.listillo.ticket.Textos.normalizar(web.nombre());
        this.formato = web.formato();
        this.precio = web.precio();
        this.tamano = web.tamano();
        this.unidad = web.unidad();
        this.precioUnidad = web.precioUnidad();
        this.seccion = web.seccion();
        this.imagenUrl = web.imagenUrl();
        this.url = web.url();
        this.actualizadoEn = LocalDateTime.now();
    }

    /** El día del precio: el que dijo la fuente o, si es de una web, el de la importación. */
    public java.time.LocalDate fecha() {
        return fechaPrecio != null ? fechaPrecio : actualizadoEn.toLocalDate();
    }

    public void adivinado(UUID genericoId) {
        if (!genericoManual) {
            this.genericoId = genericoId;
        }
    }

    public void corregir(UUID genericoId) {
        this.genericoId = genericoId;
        this.genericoManual = true;
    }

    @Embeddable
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Clave implements Serializable {

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private Cadena cadena;

        @Column(name = "id_externo", nullable = false, length = 40)
        private String idExterno;

        public Clave(Cadena cadena, String idExterno) {
            this.cadena = cadena;
            this.idExterno = idExterno;
        }
    }
}
