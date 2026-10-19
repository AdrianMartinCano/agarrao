package dev.pimon.listillo.gasolina.estaciones;

import dev.pimon.listillo.gasolina.estaciones.dto.EstacionCercanaDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.stream.Collectors;

/** Paso de dominio a DTO. Lo comparten la búsqueda y los favoritos. */
public final class EstacionMapper {

    private EstacionMapper() {
    }

    /**
     * @param distanciaKm distancia ya calculada, o null si el contexto no tiene un punto
     *                    de referencia (por ejemplo, al listar favoritos sin ubicación)
     * @param carburante  el buscado, para destacar su precio; puede ser null
     */
    public static EstacionCercanaDto aDto(Estacion estacion, Double distanciaKm, Carburante carburante) {
        Map<String, BigDecimal> precios = estacion.precios().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));

        return new EstacionCercanaDto(
                estacion.id(),
                estacion.rotulo(),
                estacion.direccion(),
                estacion.municipio(),
                estacion.provincia(),
                estacion.codigoPostal(),
                estacion.horario(),
                estacion.latitud(),
                estacion.longitud(),
                // Los metros de más o de menos no le importan a nadie eligiendo gasolinera,
                // y evita mandar 13 decimales de ruido en cada uno de los resultados.
                distanciaKm == null ? null : redondear(distanciaKm),
                carburante == null ? null : estacion.precios().get(carburante),
                precios
        );
    }

    private static double redondear(double km) {
        return BigDecimal.valueOf(km).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
