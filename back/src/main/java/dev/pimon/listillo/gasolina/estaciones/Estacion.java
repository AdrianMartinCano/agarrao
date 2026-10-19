package dev.pimon.listillo.gasolina.estaciones;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Una gasolinera con sus precios en vigor. Modelo de dominio, inmutable.
 *
 * Vive solo en memoria (ver {@link CatalogoEstaciones}): no es una entidad JPA ni se
 * guarda en la base. Los datos son públicos, cambian cada media hora y caben enteros
 * en el heap, así que persistirlos solo añadiría un esquema que mantener sincronizado
 * con el del Ministerio.
 *
 * @param id       IDEESS del Ministerio: identificador estable de la estación
 * @param precios  solo los carburantes que esta estación vende de verdad
 */
public record Estacion(
        String id,
        String rotulo,
        String direccion,
        String municipio,
        String localidad,
        String provincia,
        String codigoPostal,
        String horario,
        double latitud,
        double longitud,
        Map<Carburante, BigDecimal> precios
) {

    public Optional<BigDecimal> precio(Carburante carburante) {
        return Optional.ofNullable(precios.get(carburante));
    }

    public boolean vende(Carburante carburante) {
        return precios.containsKey(carburante);
    }
}
