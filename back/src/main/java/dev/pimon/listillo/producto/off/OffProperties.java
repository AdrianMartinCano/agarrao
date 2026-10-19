package dev.pimon.listillo.producto.off;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Open Food Facts. Pide identificarse con un User-Agent «App/versión (contacto)» y no pasar
 * de 15 consultas de producto por minuto por IP.
 *
 * @param url       la API (cambiable para pruebas o un espejo)
 * @param contacto  correo de contacto para el User-Agent; por variable de entorno, nunca en
 *                  el repo
 * @param porMinuto tope propio, por debajo del de OFF
 * @param espera    lo máximo que se espera a OFF: en el súper no se puede quedar colgado
 */
@ConfigurationProperties("listillo.off")
public record OffProperties(
        @DefaultValue("https://world.openfoodfacts.org") String url,
        @DefaultValue("") String contacto,
        @DefaultValue("12") int porMinuto,
        @DefaultValue("4s") Duration espera
) {

    public String userAgent(String version) {
        return "Listillo/" + version + (contacto.isBlank() ? "" : " (" + contacto + ")");
    }
}
