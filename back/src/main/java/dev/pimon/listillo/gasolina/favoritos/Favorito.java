package dev.pimon.listillo.gasolina.favoritos;

import dev.pimon.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una gasolinera que un usuario ha marcado como favorita.
 *
 * Guarda solo la referencia ({@code estacionId} es el IDEESS del Ministerio), nunca una
 * copia de la estación: los precios cambian cada media hora y una copia quedaría vieja
 * al momento. Los datos se rellenan al vuelo desde el catálogo en memoria.
 *
 * Tampoco hay relación JPA con AppUser, solo el id: el usuario vive en una librería
 * compartida y atarla con una @ManyToOne obligaría a este proyecto a conocer su mapeo.
 * La integridad la garantiza la clave ajena de la migración.
 */
@Entity
@Table(
        name = "favoritos",
        // Sin esto, pulsar dos veces el botón de favorito crearía dos filas iguales.
        // Se defiende en la base y no solo en el servicio, que es donde de verdad aguanta
        // dos peticiones simultáneas.
        uniqueConstraints = @UniqueConstraint(
                name = "uk_favoritos_usuario_estacion",
                columnNames = {"user_id", "estacion_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class Favorito extends BaseEntity {

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    /** IDEESS de la estación en el catálogo del Ministerio. */
    @Column(name = "estacion_id", nullable = false, length = 32)
    private String estacionId;

    public Favorito(String userId, String estacionId) {
        this.userId     = userId;
        this.estacionId = estacionId;
    }
}
