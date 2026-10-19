package dev.pimon.listillo.generico;

import dev.pimon.listillo.ticket.Cadena;
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

/**
 * Qué genérico es un producto de ticket para un hogar. El producto es el texto del ticket
 * en esa cadena («leche semi hacend 6x1l» en Mercadona), como en el aprendizaje de tickets:
 * dentro de una cadena, el mismo texto es el mismo producto.
 */
@Entity
@Table(name = "clasificaciones")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Clasificacion {

    @EmbeddedId
    private Clave clave;

    /** Null con origen USUARIO: «no es ningún genérico». */
    @Column(name = "generico_id")
    private UUID genericoId;

    @Column(precision = 10, scale = 3)
    private BigDecimal tamano;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private OrigenClasificacion origen;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    public static Clasificacion adivinada(Clave clave, UUID genericoId) {
        Clasificacion c = new Clasificacion();
        c.clave = clave;
        c.genericoId = genericoId;
        c.origen = OrigenClasificacion.AUTO;
        c.actualizadoEn = LocalDateTime.now();
        return c;
    }

    public static Clasificacion aMano(Clave clave) {
        Clasificacion c = new Clasificacion();
        c.clave = clave;
        c.origen = OrigenClasificacion.USUARIO;
        c.actualizadoEn = LocalDateTime.now();
        return c;
    }

    /** Una persona lo corrige: desde ahora manda lo suyo. */
    public void corregir(UUID genericoId, BigDecimal tamano) {
        this.genericoId = genericoId;
        this.tamano = tamano;
        this.origen = OrigenClasificacion.USUARIO;
        this.actualizadoEn = LocalDateTime.now();
    }

    @Embeddable
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Clave implements Serializable {

        @Column(name = "hogar_id", nullable = false)
        private UUID hogarId;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private Cadena cadena;

        /** Normalizado con {@code Textos.normalizar}. */
        @Column(nullable = false, length = 120)
        private String texto;

        public Clave(UUID hogarId, Cadena cadena, String texto) {
            this.hogarId = hogarId;
            this.cadena = cadena;
            this.texto = texto;
        }
    }
}
