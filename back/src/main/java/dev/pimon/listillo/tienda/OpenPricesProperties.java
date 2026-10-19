package dev.pimon.listillo.tienda;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Open Prices (prices.openfoodfacts.org): precios que sube la gente desde sus tickets y
 * etiquetas, datos abiertos (licencia ODbL: hay que citar la fuente). ENCENDIDO por defecto:
 * no es leer la web de nadie, es una API pública pensada para reutilizarse.
 *
 * Solo rellena las cadenas cuya web no se lee (Carrefour, Dia, Alcampo…, o todas si
 * {@code PRECIOS_WEB} está apagado): la web, cuando se lee, es más completa y es de hoy.
 *
 * @param activo encendido con {@code OPEN_PRICES=true} (por defecto)
 * @param cron   cuándo se importa (por defecto, cada noche a las 5:15, tras las webs)
 * @param url    la API (cambiable para pruebas)
 * @param dias   cuántos días atrás vale un precio; más viejo no dice nada de hoy
 * @param pausa  entre petición y petición: es un proyecto sin ánimo de lucro
 * @param espera lo máximo que se espera a una respuesta
 */
@ConfigurationProperties("listillo.open-prices")
public record OpenPricesProperties(
        @DefaultValue("true") boolean activo,
        @DefaultValue("0 15 5 * * *") String cron,
        @DefaultValue("https://prices.openfoodfacts.org") String url,
        @DefaultValue("365") int dias,
        @DefaultValue("1s") Duration pausa,
        @DefaultValue("30s") Duration espera
) {
}
