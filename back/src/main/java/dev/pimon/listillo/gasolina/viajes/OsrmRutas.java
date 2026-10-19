package dev.pimon.listillo.gasolina.viajes;

import com.fasterxml.jackson.databind.JsonNode;
import dev.pimon.listillo.gasolina.estaciones.GasolinaProperties;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Rutas con OSRM (Open Source Routing Machine), sobre los mapas de OpenStreetMap.
 *
 * Se pide {@code overview=full}: la línea entera, no la simplificada, porque con ella se
 * mide a qué distancia de la carretera queda cada gasolinera; con la simplificada, en una
 * curva larga una gasolinera pegada a la autovía podía salir «a 3 km».
 *
 * OSRM habla en {@code longitud,latitud} (al revés que casi todo): ojo al leer y al pedir.
 */
@Component
public class OsrmRutas implements CalculadorRutas {

    /** Su política de uso pide identificarse en el User-Agent. */
    private static final String AGENTE = "Agarrao/1.0 (https://agarrao.pimon.dev)";

    private final RestClient cliente;

    public OsrmRutas(GasolinaProperties props) {
        var http = HttpClient.newBuilder()
                .connectTimeout(props.rutas().timeout())
                .build();
        var fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(props.rutas().timeout());
        this.cliente = RestClient.builder()
                .baseUrl(props.rutas().url())
                .requestFactory(fabrica)
                .defaultHeader("User-Agent", AGENTE)
                .build();
    }

    @Override
    public Optional<Ruta> calcular(double origenLat, double origenLon, double destinoLat, double destinoLon) {
        String coordenadas = String.format(Locale.ROOT, "%.6f,%.6f;%.6f,%.6f",
                origenLon, origenLat, destinoLon, destinoLat);
        JsonNode respuesta;
        try {
            respuesta = cliente.get()
                    .uri("/route/v1/driving/{coordenadas}?overview=full&geometries=geojson", coordenadas)
                    .retrieve()
                    // OSRM contesta 400 con {"code":"NoRoute"} cuando no hay camino: no es un fallo
                    .onStatus(status -> status.value() == 400, (peticion, r) -> { })
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new RutasNoDisponiblesException("No se pudo consultar el servicio de rutas", e);
        }
        return leer(respuesta);
    }

    /** Separado para probarlo con una respuesta de OSRM guardada. */
    static Optional<Ruta> leer(JsonNode respuesta) {
        if (respuesta == null) {
            throw new RutasNoDisponiblesException("El servicio de rutas respondió vacío", null);
        }
        String codigo = respuesta.path("code").asText();
        if ("NoRoute".equals(codigo) || "NoSegment".equals(codigo)) {
            return Optional.empty();
        }
        JsonNode ruta = respuesta.path("routes").path(0);
        if (!"Ok".equals(codigo) || ruta.isMissingNode()) {
            throw new RutasNoDisponiblesException("El servicio de rutas respondió " + codigo, null);
        }
        List<double[]> puntos = new ArrayList<>();
        for (JsonNode p : ruta.path("geometry").path("coordinates")) {
            puntos.add(new double[]{p.get(1).asDouble(), p.get(0).asDouble()});
        }
        return Optional.of(new Ruta(
                ruta.path("distance").asDouble() / 1000,
                ruta.path("duration").asDouble() / 60,
                puntos));
    }
}
