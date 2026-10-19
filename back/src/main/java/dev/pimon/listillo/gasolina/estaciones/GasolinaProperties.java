package dev.pimon.listillo.gasolina.estaciones;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Configuración del proyecto, tipada y en un solo sitio (prefijo {@code listillo.gasolina}).
 * Los valores por defecto son los de producción; en application.yml solo se tocan
 * los que interese cambiar.
 */
@ConfigurationProperties(prefix = "listillo.gasolina")
public record GasolinaProperties(
        @DefaultValue Minetur minetur,
        @DefaultValue Busqueda busqueda,
        @DefaultValue Rutas rutas
) {

    /**
     * @param url       endpoint público de precios de carburantes (todas las estaciones)
     * @param refresco  cada cuánto se recarga el catálogo. El Ministerio actualiza cada
     *                  30 minutos; pedirlo más a menudo solo gasta ancho de banda.
     * @param timeout   corte de la llamada. La respuesta son ~12 MB y el servicio tiene
     *                  días malos; sin timeout, un cuelgue del Ministerio dejaría el hilo
     *                  de refresco bloqueado para siempre.
     */
    public record Minetur(
            @DefaultValue("https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/EstacionesTerrestres/")
            String url,
            @DefaultValue("30m") Duration refresco,
            @DefaultValue("60s") Duration timeout,
            /** false en los tests: no se descarga nada del Ministerio. */
            @DefaultValue("true") boolean activo
    ) {
    }

    /**
     * @param radioPorDefectoKm  radio que se usa si el cliente no pide uno
     * @param radioMaximoKm      tope duro. Sin él, un radio de 3.000 km devolvería el país
     *                           entero ordenado, que no es una búsqueda "por cercanía".
     * @param tamanoPaginaMaximo tope de resultados por página, para que nadie pida 10.000
     */
    public record Busqueda(
            @DefaultValue("5") double radioPorDefectoKm,
            @DefaultValue("50") double radioMaximoKm,
            @DefaultValue("100") int tamanoPaginaMaximo
    ) {
    }

    /**
     * Cálculo de rutas para Viajes (OSRM, sobre OpenStreetMap; sin clave).
     *
     * @param url      el servidor público de demostración de OSRM por defecto. Su política
     *                 pide poco uso (≤ 1 petición/s) y un User-Agent que identifique: para
     *                 una app de casa sobra. Si algún día se queda corto, se monta uno propio.
     * @param timeout  corte de la llamada: si OSRM tarda, mejor decir que no hay ruta ahora
     */
    public record Rutas(
            @DefaultValue("https://router.project-osrm.org") String url,
            @DefaultValue("10s") Duration timeout,
            @DefaultValue("50") int maxEstaciones
    ) {
    }
}
