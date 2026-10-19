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
import java.util.UUID;

/** Un producto de las webs que la casa ha dicho que no es su genérico propio. */
@Entity
@Table(name = "descartes_web")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DescarteWeb {

    @EmbeddedId
    private Clave clave;

    public DescarteWeb(UUID genericoId, Cadena cadena, String idExterno) {
        this.clave = new Clave(genericoId, cadena, idExterno);
    }

    @Embeddable
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Clave implements Serializable {

        @Column(name = "generico_id", nullable = false)
        private UUID genericoId;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private Cadena cadena;

        @Column(name = "id_externo", nullable = false, length = 40)
        private String idExterno;

        public Clave(UUID genericoId, Cadena cadena, String idExterno) {
            this.genericoId = genericoId;
            this.cadena = cadena;
            this.idExterno = idExterno;
        }
    }
}
