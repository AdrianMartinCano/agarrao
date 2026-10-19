package dev.pimon.listillo.ticket;

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
import java.time.LocalDateTime;

/**
 * Lo aprendido por un hogar: en esta cadena, este texto de ticket (normalizado) es este
 * producto. El primer ticket de Mercadona se casa a mano y los siguientes salen casados
 * solos. Es de cada hogar (V7): compartido, un casado equivocado ensuciaba a todos.
 */
@Entity
@Table(name = "alias_ticket")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AliasTicket {

    @EmbeddedId
    private Clave clave;

    @Column(length = 14)
    private String ean;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false)
    private int veces;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    public AliasTicket(java.util.UUID hogarId, Cadena cadena, String textoNormalizado, String ean, String nombre) {
        this.clave = new Clave(hogarId, cadena, textoNormalizado);
        this.ean = ean;
        this.nombre = nombre;
        this.veces = 1;
        this.actualizadoEn = LocalDateTime.now();
    }

    /** Otra vez lo mismo suma confianza; si alguien lo casa con otra cosa, gana lo último. */
    public void confirmar(String ean, String nombre) {
        if (java.util.Objects.equals(this.ean, ean) && this.nombre.equals(nombre)) {
            this.veces++;
        } else {
            this.ean = ean;
            this.nombre = nombre;
            this.veces = 1;
        }
        this.actualizadoEn = LocalDateTime.now();
    }

    @Embeddable
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Clave implements Serializable {

        @Column(name = "hogar_id", nullable = false)
        private java.util.UUID hogarId;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private Cadena cadena;

        @Column(nullable = false, length = 120)
        private String texto;

        public Clave(java.util.UUID hogarId, Cadena cadena, String texto) {
            this.hogarId = hogarId;
            this.cadena = cadena;
            this.texto = texto;
        }
    }
}
