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

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un repostaje: qué echaste, dónde, cuánto costó y con cuántos kilómetros.
 *
 * DOS DECISIONES QUE MERECEN EXPLICACIÓN:
 *
 * 1. Se guardan el nombre y la dirección de la gasolinera, no solo su id. Parece
 *    duplicar datos que ya están en el catálogo, pero el catálogo es una foto de HOY: si
 *    esa gasolinera cierra, desaparece del listado del Ministerio y el historial se
 *    quedaría lleno de "estación desconocida". Un histórico tiene que sostenerse solo.
 *
 * 2. Se guardan los precios de referencia EN EL MOMENTO del repostaje. Son números que
 *    solo se pueden saber ese día —el Ministerio no publica histórico— y salen gratis,
 *    porque el catálogo ya está en memoria. Con ellos se puede decir "pagaste 1,68 cuando
 *    la de al lado estaba a 1,66", que es lo que convierte un cuaderno de gastos en algo
 *    que dice algo.
 */
@Entity
@Table(name = "repostajes")
@Getter
@Setter
@NoArgsConstructor
public class Repostaje extends BaseEntity {

    @Column(name = "vehiculo_id", nullable = false, length = 36)
    private String vehiculoId;

    /** Se guarda para poder filtrar por usuario sin pasar por la tabla de vehículos. */
    @Column(name = "hogar_id", nullable = false)
    private java.util.UUID hogarId;

    /** Quién lo apuntó (sin FK: si borra su cuenta, el repostaje se queda en el hogar). */
    @Column(name = "creado_por", length = 36)
    private String creadoPor;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "estacion_id", length = 32)
    private String estacionId;

    /** Congelados: ver el comentario de la clase. */
    @Column(name = "estacion_nombre", length = 120)
    private String estacionNombre;

    @Column(name = "estacion_municipio", length = 120)
    private String estacionMunicipio;

    /** Desde la 1.1.5. Los anteriores la sacan del catálogo al leerlos (si sigue abierta). */
    @Column(name = "estacion_direccion", length = 200)
    private String estacionDireccion;

    @Column(nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private Carburante carburante;

    /** Litros repostados. Con tres decimales, como los marca el surtidor. */
    @Column(nullable = false, precision = 8, scale = 3)
    private BigDecimal litros;

    @Column(name = "precio_litro", nullable = false, precision = 6, scale = 3)
    private BigDecimal precioLitro;

    /** Lo que se pagó. Se guarda además de calcularse, para que cuadre con el ticket. */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal importe;

    /**
     * Kilómetros del cuentakilómetros. Opcional: sin él, este repostaje cuenta para el
     * gasto pero no sirve para medir consumo.
     */
    private Integer kilometros;

    /**
     * Si se llenó el depósito hasta arriba.
     *
     * Es lo que hace que el consumo sea correcto y no un número inventado. El cálculo
     * solo cuadra entre dos llenados: saliendo de lleno y volviendo a lleno, los litros
     * que metes son exactamente los que gastaste. Los repostajes parciales NO se tiran:
     * sus litros se acumulan hasta el siguiente lleno (ver CalculadoraConsumo).
     */
    @Column(name = "deposito_lleno", nullable = false)
    private boolean depositoLleno = true;

    /**
     * Precio medio de ese carburante en esa provincia el día del repostaje.
     *
     * Se conserva por el histórico: es lo único con lo que se pueden comparar los
     * repostajes anteriores a que existiera la zona. Para los nuevos, la referencia buena
     * es {@link #precioMedioZona}, porque una gasolinera a noventa kilómetros nunca fue
     * una alternativa de verdad.
     */
    @Column(name = "precio_medio_provincia", precision = 6, scale = 3)
    private BigDecimal precioMedioProvincia;

    /** Media de ese carburante alrededor de la gasolinera, dentro de {@link #radioZonaKm}. */
    @Column(name = "precio_medio_zona", precision = 6, scale = 3)
    private BigDecimal precioMedioZona;

    /**
     * Lo que costaba el litro en la más barata de esa misma zona.
     *
     * Es el número que hace accionable el histórico: la media dice si lo hiciste regular,
     * el mínimo dice cuánto dejaste sobre la mesa, y eso segundo sí se puede corregir la
     * próxima vez.
     */
    @Column(name = "precio_minimo_zona", precision = 6, scale = 3)
    private BigDecimal precioMinimoZona;

    /** Radio con el que se calcularon los dos de arriba: no siempre es el mismo. */
    @Column(name = "radio_zona_km")
    private Integer radioZonaKm;

    @Column(length = 300)
    private String notas;
}
