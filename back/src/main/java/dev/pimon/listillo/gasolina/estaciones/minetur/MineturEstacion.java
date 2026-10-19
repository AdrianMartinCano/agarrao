package dev.pimon.listillo.gasolina.estaciones.minetur;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.pimon.listillo.gasolina.estaciones.Carburante;
import dev.pimon.listillo.gasolina.estaciones.Estacion;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Una estación tal y como la manda el Ministerio, antes de tocarla.
 *
 * Es un DTO del borde y por eso es una clase mutable con setters, no un record: el JSON
 * de origen tiene claves con espacios, tildes, puntos y paréntesis ("Longitud (WGS84)",
 * "C.P."), y los precios son ~23 campos distintos que además cambian de vez en cuando.
 * Con {@link JsonAnySetter} los recogemos todos de golpe en un mapa en vez de declarar
 * veintitrés propiedades.
 *
 * Dos rarezas del origen que se normalizan aquí y no salen de este paquete:
 *   - los decimales vienen con COMA ("1,779", "40,955111");
 *   - lo que no se vende viene como cadena vacía, no como null ni como 0.
 */
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MineturEstacion {

    @JsonProperty("IDEESS")            private String ideess;
    @JsonProperty("Rótulo")            private String rotulo;
    @JsonProperty("Dirección")         private String direccion;
    @JsonProperty("Municipio")         private String municipio;
    @JsonProperty("Localidad")         private String localidad;
    @JsonProperty("Provincia")         private String provincia;
    @JsonProperty("C.P.")              private String codigoPostal;
    @JsonProperty("Horario")           private String horario;
    @JsonProperty("Latitud")           private String latitud;
    @JsonProperty("Longitud (WGS84)")  private String longitud;

    private final Map<Carburante, BigDecimal> precios = new EnumMap<>(Carburante.class);

    /**
     * Recoge los campos "Precio *". Jackson manda aquí todo lo que no encaja arriba;
     * lo que no sea un precio conocido se descarta en silencio.
     */
    @JsonAnySetter
    void campoDesconocido(String clave, Object valor) {
        if (!(valor instanceof String texto) || !clave.startsWith("Precio ")) {
            return;
        }
        Carburante.porCampoMinetur(clave)
                .ifPresent(carburante -> aDecimal(texto)
                        .ifPresent(precio -> precios.put(carburante, precio)));
    }

    /**
     * Pasa a nuestro modelo de dominio.
     *
     * Devuelve vacío para las estaciones que no nos sirven: sin coordenadas usables no
     * se puede calcular una distancia, y sin ningún precio publicado no hay nada que
     * enseñar. Se descartan aquí, una vez, en lugar de arrastrar nulos por toda la app.
     */
    public Optional<Estacion> aEstacion() {
        Optional<BigDecimal> lat = aDecimal(latitud);
        Optional<BigDecimal> lon = aDecimal(longitud);
        if (lat.isEmpty() || lon.isEmpty() || precios.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Estacion(
                ideess,
                limpiar(rotulo),
                limpiar(direccion),
                limpiar(municipio),
                limpiar(localidad),
                limpiar(provincia),
                limpiar(codigoPostal),
                limpiar(horario),
                lat.get().doubleValue(),
                lon.get().doubleValue(),
                Map.copyOf(precios)
        ));
    }

    /** "1,779" → 1.779; "" o basura → vacío. */
    private static Optional<BigDecimal> aDecimal(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(valor.trim().replace(',', '.')));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** El origen trae sobras de espacios y algún campo a null. */
    private static String limpiar(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
