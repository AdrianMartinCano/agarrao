package dev.pimon.listillo.gasolina.estaciones.minetur;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Sobre de la respuesta del Ministerio.
 *
 * "ResultadoConsulta" vale "OK" cuando ha ido bien; si no, la lista puede venir vacía
 * con un HTTP 200, así que hay que mirarlo (lo hace {@link MineturClient}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MineturRespuesta(
        @JsonProperty("Fecha")             String fecha,
        @JsonProperty("ResultadoConsulta") String resultadoConsulta,
        @JsonProperty("ListaEESSPrecio")   List<MineturEstacion> estaciones
) {
    public boolean correcta() {
        return "OK".equalsIgnoreCase(resultadoConsulta);
    }
}
