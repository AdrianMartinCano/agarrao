package dev.pimon.listillo.gasolina.estaciones;

import dev.pimon.listillo.gasolina.estaciones.minetur.MineturClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * El catálogo de estaciones que sirve la aplicación, guardado en memoria.
 *
 * POR QUÉ ASÍ. El origen son ~11.000 estaciones (unos 12 MB de JSON) que el Ministerio
 * actualiza cada media hora. Descargarlo en cada petición sería lentísimo y dejaría la
 * web a merced de que su servidor conteste; guardarlo en Postgres obligaría a mantener
 * un esquema sincronizado con el suyo para datos que caducan en 30 minutos. Con una
 * copia en memoria refrescada en segundo plano, una búsqueda no sale de este proceso.
 *
 * EL CASO IMPORTANTE es el fallo: si un refresco falla, se conserva la copia anterior y
 * se sigue sirviendo con datos algo viejos. Una caída del Ministerio no debe ser una
 * caída de la web. Lo único que no se puede tapar es que falle la PRIMERA carga, porque
 * entonces no hay nada que servir.
 *
 * Sobre concurrencia: la copia se publica entera de golpe en un campo {@code volatile}
 * con listas inmutables. Quien esté buscando mientras entra un refresco termina con la
 * copia vieja y coherente, en vez de ver una lista a medio rellenar.
 */
@Slf4j
@Component
public class CatalogoEstaciones {

    private final MineturClient cliente;
    private final GasolinaProperties props;

    private volatile Copia copia = Copia.inicial();

    public CatalogoEstaciones(MineturClient cliente, GasolinaProperties props) {
        this.cliente = cliente;
        this.props = props;
    }

    /**
     * Carga inicial y recargas posteriores.
     *
     * No hace falta ningún @EventListener(ApplicationReadyEvent) que fuerce la primera
     * descarga: @Scheduled con fixedDelay YA se ejecuta una vez nada más arrancar. Al
     * tener las dos cosas, la aplicación se bajaba los 12 MB dos veces seguidas.
     *
     * Es {@code fixedDelay} y no {@code fixedRate} porque cuenta desde que TERMINA la
     * anterior: si una descarga tarda más de la cuenta, no se encadenan refrescos
     * solapados bajándose el listado entero a la vez.
     *
     * Que la primera carga vaya en el planificador y no en el constructor es a posta: el
     * arranque no se retrasa ni se cae si el Ministerio está fuera de servicio. La
     * aplicación levanta y las búsquedas responden 503 con un mensaje claro hasta que
     * entre la primera descarga buena.
     */
    @Scheduled(fixedDelayString = "${listillo.gasolina.minetur.refresco:30m}")
    public void refrescar() {
        if (!props.minetur().activo()) {
            return;
        }
        try {
            List<Estacion> estaciones = cliente.descargarEstaciones();
            copia = Copia.de(estaciones);
            log.info("Catálogo actualizado: {} estaciones, {} lugares para buscar",
                    estaciones.size(), copia.lugares().tamano());
        } catch (RuntimeException e) {
            // A propósito no se propaga: el planificador dejaría de reintentar y una
            // caída pasajera del origen se volvería permanente.
            if (copia.vacia()) {
                log.error("No se pudo cargar el catálogo y no hay copia previa: las búsquedas fallarán hasta el próximo intento", e);
            } else {
                log.warn("Falló el refresco del catálogo; se sigue sirviendo la copia de {} ({} estaciones): {}",
                        copia.actualizado(), copia.estaciones().size(), e.getMessage());
            }
        }
    }

    /** Todas las estaciones. Lista inmutable; nunca null. */
    public List<Estacion> todas() {
        return copia.estaciones();
    }

    public Optional<Estacion> porId(String id) {
        return Optional.ofNullable(copia.indice().get(id));
    }

    /** Instante de la última descarga correcta; vacío si aún no ha habido ninguna. */
    public Optional<Instant> ultimaActualizacion() {
        return Optional.ofNullable(copia.actualizado());
    }

    /** Municipios y códigos postales desde los que se puede buscar. */
    public IndiceLugares lugares() {
        return copia.lugares();
    }

    public boolean disponible() {
        return !copia.vacia();
    }

    /**
     * Una foto coherente del catálogo. Los índices (por id y de lugares) se calculan una
     * vez al refrescar, no en cada consulta: agrupar once mil estaciones por municipio en
     * cada pulsación del teclado del buscador de lugares sería un despropósito.
     */
    private record Copia(List<Estacion> estaciones, Map<String, Estacion> indice,
                         IndiceLugares lugares, Instant actualizado) {

        /** La copia con la que arranca la aplicación, antes de la primera descarga. */
        static Copia inicial() {
            return new Copia(List.of(), Map.of(), IndiceLugares.vacio(), null);
        }

        static Copia de(List<Estacion> estaciones) {
            Map<String, Estacion> indice = estaciones.stream()
                    // El IDEESS es único, pero si el origen colara un duplicado nos
                    // quedamos con el primero en vez de reventar con IllegalStateException.
                    .collect(Collectors.toUnmodifiableMap(Estacion::id, Function.identity(), (a, b) -> a));
            return new Copia(List.copyOf(estaciones), indice, IndiceLugares.de(estaciones), Instant.now());
        }

        boolean vacia() {
            return estaciones.isEmpty();
        }
    }
}
