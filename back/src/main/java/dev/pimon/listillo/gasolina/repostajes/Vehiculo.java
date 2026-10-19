package dev.pimon.listillo.gasolina.repostajes;

import dev.pimon.common.entity.BaseEntity;
import dev.pimon.listillo.gasolina.estaciones.Carburante;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un coche (o moto, o furgoneta) de un usuario.
 *
 * NO ES UN LUJO PARA QUIEN TENGA DOS COCHES: sin separar por vehículo, el consumo no se
 * puede calcular. Los kilómetros salen del cuentakilómetros de cada coche, así que
 * mezclar los de dos vehículos da saltos imposibles — registrar 40.000 km del coche y
 * luego 12.000 de la moto parecería haber retrocedido 28.000 km.
 */
@Entity
@Table(name = "vehiculos")
@Getter
@Setter
@NoArgsConstructor
public class Vehiculo extends BaseEntity {

    @Column(name = "hogar_id", nullable = false)
    private java.util.UUID hogarId;

    /** Como lo llama su dueño: "El Golf", "La moto". */
    @Column(nullable = false, length = 60)
    private String nombre;

    /** El que reposta habitualmente. Se usa para preseleccionarlo en el formulario. */
    @Column(nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private Carburante carburante;

    /** Opcional: hay quien distingue sus coches por la matrícula y quien por el nombre. */
    @Column(length = 15)
    private String matricula;

    /**
     * Los vehículos no se borran, se archivan.
     *
     * Borrar un coche vendido se llevaría por delante su historial de repostajes, que es
     * justo lo que alguien querría conservar: cuánto gastaba, cuánto consumía.
     */
    @Column(nullable = false)
    private boolean activo = true;

    public Vehiculo(java.util.UUID hogarId, String nombre, Carburante carburante, String matricula) {
        this.hogarId    = hogarId;
        this.nombre     = nombre;
        this.carburante = carburante;
        this.matricula  = matricula;
    }
}
