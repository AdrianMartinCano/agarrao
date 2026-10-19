package dev.pimon.listillo.gasolina.repostajes.dto;

import dev.pimon.listillo.gasolina.estaciones.Carburante;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Los DTOs de vehículos y repostajes, juntos por ser todos records de pocas líneas que
 * solo se usan a la vez. Repartirlos en ocho ficheros de cinco líneas costaría más de
 * leer que esto.
 */
public final class RepostajesDtos {

    private RepostajesDtos() {
    }

    // ── Vehículos ── //

    public record CrearVehiculoRequest(
            @NotBlank(message = "Ponle un nombre al vehículo")
            @Size(max = 60, message = "El nombre es demasiado largo")
            String nombre,

            @NotNull(message = "Elige el carburante que usa")
            Carburante carburante,

            @Size(max = 15, message = "La matrícula es demasiado larga")
            String matricula
    ) {
    }

    /**
     * Cambiar datos de un vehículo ya creado: una matrícula mal tecleada, el carburante
     * equivocado al darlo de alta.
     *
     * Cambiar el carburante NO reescribe el historial: cada repostaje guarda el suyo, así
     * que lo ya registrado se queda como estaba. Solo cambia lo que se propone la próxima
     * vez. Es lo correcto — si alguien se equivocó al dar de alta el coche, sus repostajes
     * anteriores sí llevaban el carburante bueno.
     */
    public record ActualizarVehiculoRequest(
            @NotBlank(message = "Ponle un nombre al vehículo")
            @Size(max = 60, message = "El nombre es demasiado largo")
            String nombre,

            @NotNull(message = "Elige el carburante que usa")
            Carburante carburante,

            @Size(max = 15, message = "La matrícula es demasiado larga")
            String matricula
    ) {
    }

    /**
     * @param consumoMedio  l/100 km de todo el historial; null hasta que haya dos
     *                      llenados completos con kilómetros
     * @param repostajes    cuántos lleva registrados
     */
    public record VehiculoDto(
            String id,
            String nombre,
            String carburante,
            String carburanteEtiqueta,
            String matricula,
            BigDecimal consumoMedio,
            int repostajes,
            BigDecimal gastoTotal
    ) {
    }

    // ── Repostajes ── //

    public record CrearRepostajeRequest(
            @NotBlank(message = "Elige el vehículo")
            String vehiculoId,

            @NotNull(message = "La fecha es obligatoria")
            // Un repostaje futuro es siempre una errata al teclear el año.
            @PastOrPresent(message = "La fecha no puede ser futura")
            LocalDate fecha,

            @NotNull(message = "Elige el carburante")
            Carburante carburante,

            @NotNull(message = "Indica los litros")
            @DecimalMin(value = "0.01", message = "Los litros deben ser mayores que cero")
            BigDecimal litros,

            @NotNull(message = "Indica el precio por litro")
            @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero")
            BigDecimal precioLitro,

            /**
             * Opcional: lo que pone el ticket. Si viene, se guarda tal cual (20 € son 20,00 €,
             * no lo que salga de litros × precio redondeado); si no, se calcula.
             */
            @DecimalMin(value = "0.01", message = "El importe debe ser mayor que cero")
            BigDecimal importe,

            /** Opcional: sin kilómetros el repostaje cuenta para el gasto, no para el consumo. */
            @Min(value = 0, message = "Los kilómetros no pueden ser negativos")
            Integer kilometros,

            /** Por defecto true: la mayoría de la gente llena el depósito. */
            Boolean depositoLleno,

            /** Opcional: si viene, se congelan sus datos y se calcula la media provincial. */
            String estacionId,

            @Size(max = 300, message = "Las notas son demasiado largas")
            String notas
    ) {
    }

    /**
     * Corregir un repostaje ya apuntado: unos kilómetros mal tecleados, el importe
     * cambiado, la gasolinera equivocada.
     *
     * Lleva los mismos campos que crear, incluido el vehículo: quien apunta un repostaje
     * en el coche cuando era de la moto tiene que poder moverlo sin borrarlo y rehacerlo.
     */
    public record ActualizarRepostajeRequest(
            @NotBlank(message = "Elige el vehículo")
            String vehiculoId,

            @NotNull(message = "La fecha es obligatoria")
            @PastOrPresent(message = "La fecha no puede ser futura")
            LocalDate fecha,

            @NotNull(message = "Elige el carburante")
            Carburante carburante,

            @NotNull(message = "Indica los litros")
            @DecimalMin(value = "0.01", message = "Los litros deben ser mayores que cero")
            BigDecimal litros,

            @NotNull(message = "Indica el precio por litro")
            @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero")
            BigDecimal precioLitro,

            /**
             * Opcional: lo que pone el ticket. Si viene, se guarda tal cual (20 € son 20,00 €,
             * no lo que salga de litros × precio redondeado); si no, se calcula.
             */
            @DecimalMin(value = "0.01", message = "El importe debe ser mayor que cero")
            BigDecimal importe,

            @Min(value = 0, message = "Los kilómetros no pueden ser negativos")
            Integer kilometros,

            Boolean depositoLleno,

            String estacionId,

            @Size(max = 300, message = "Las notas son demasiado largas")
            String notas
    ) {
    }

    /**
     * @param precioMedioProvincia  media en la provincia ESE día. Solo sirve ya para el
     *                              histórico anterior a que se guardara la zona
     * @param precioMedioZona       media alrededor de la gasolinera, en {@code radioZonaKm}
     * @param precioMinimoZona      lo que costaba en la más barata de esa zona
     * @param radioZonaKm           radio con el que se calcularon las dos anteriores: se
     *                              ensancha donde hay pocas gasolineras, así que no es fijo
     * @param diferenciaMedia       euros del depósito entero ahorrados (POSITIVO) o pagados
     *                              de más (negativo) frente a la zona, o frente a la
     *                              provincia si el repostaje es de los antiguos
     * @param dejadoEnLaMesa        euros que habría costado menos yendo a la más barata de
     *                              la zona. Nunca negativo: cero significa que fuiste a ella
     */
    public record RepostajeDto(
            String id,
            String vehiculoId,
            String vehiculoNombre,
            LocalDate fecha,
            /** Lo necesita el front para conservar la gasolinera al editar sin tocarla. */
            String estacionId,
            String estacionNombre,
            String estacionMunicipio,
            /** Calle y número, para saber cuál era sin abrir el mapa. */
            String estacionDireccion,
            String carburante,
            String carburanteEtiqueta,
            BigDecimal litros,
            BigDecimal precioLitro,
            BigDecimal importe,
            Integer kilometros,
            boolean depositoLleno,
            BigDecimal precioMedioProvincia,
            BigDecimal precioMedioZona,
            BigDecimal precioMinimoZona,
            Integer radioZonaKm,
            BigDecimal diferenciaMedia,
            BigDecimal dejadoEnLaMesa,
            String notas
    ) {
    }

    // ── Estadísticas ── //

    /**
     * @param consumo   l/100 km del tramo
     * @param hasta     fecha del llenado que cierra el tramo: es el punto del eje X
     */
    public record TramoConsumoDto(LocalDate hasta, int kilometros, BigDecimal litros, BigDecimal consumo) {
    }

    /** Un mes del gráfico de gasto. */
    public record GastoMensualDto(String mes, BigDecimal importe, BigDecimal litros) {
    }

    /**
     * Todo lo que hace falta para pintar las gráficas de un vehículo.
     *
     * @param precios               lo pagado por litro en cada repostaje, para ver cómo
     *                              sube y baja
     * @param ahorroFrenteALaMedia  el resumen amable: cuánto se lleva ahorrado frente a la
     *                              media de la zona
     * @param dejadoEnLaMesa        el resumen accionable: cuánto MÁS se habría ahorrado
     *                              yendo siempre a la más barata que había cerca
     */
    public record EstadisticasDto(
            BigDecimal consumoMedio,
            BigDecimal gastoTotal,
            BigDecimal litrosTotales,
            int kilometrosRecorridos,
            BigDecimal ahorroFrenteALaMedia,
            BigDecimal dejadoEnLaMesa,
            List<TramoConsumoDto> consumos,
            List<GastoMensualDto> gastoMensual,
            List<PrecioPagadoDto> precios
    ) {
    }

    /**
     * @param precioReferencia  la media de la zona, o la de la provincia en los repostajes
     *                          antiguos que no la tienen. Puede ser null: los registrados
     *                          sin gasolinera no se comparan con nada
     */
    public record PrecioPagadoDto(LocalDate fecha, BigDecimal precioLitro, BigDecimal precioReferencia) {
    }
}
