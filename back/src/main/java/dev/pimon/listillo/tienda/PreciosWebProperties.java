package dev.pimon.listillo.tienda;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Leer los precios de las webs de los súper. APAGADO por defecto: tiene riesgo legal si la
 * app se publica; solo para uso privado.
 *
 * @param activo      encendido con {@code PRECIOS_WEB=true}
 * @param cron        cuándo se importa (por defecto, cada noche a las 4:30)
 * @param pausa       entre petición y petición, para no cargar la web de nadie
 * @param mercadona   almacén de Mercadona cuyos precios se leen (mad1 = Madrid)
 * @param lidl        la web de Lidl (solo publica parte de la alimentación)
 * @param consum      la tienda online de Consum
 * @param ahorramas   la tienda online de Ahorramás
 * @param eroski      el supermercado online de Eroski
 * @param aldi        la web de productos de Aldi (región península)
 * @param caprabo     la tienda online de Caprabo (la de Eroski)
 *
 * Dia y Carrefour no: sus webs bloquean la lectura automática (Akamai y Cloudflare) y no se
 * va a burlar esa protección.
 */
@ConfigurationProperties("listillo.precios-web")
public record PreciosWebProperties(
        @DefaultValue("false") boolean activo,
        @DefaultValue("0 30 4 * * *") String cron,
        @DefaultValue("400ms") Duration pausa,
        @DefaultValue("10s") Duration espera,
        @DefaultValue Mercadona mercadona,
        @DefaultValue Lidl lidl,
        @DefaultValue Consum consum,
        @DefaultValue Ahorramas ahorramas,
        @DefaultValue Eroski eroski,
        @DefaultValue Aldi aldi,
        @DefaultValue Caprabo caprabo
) {
    public record Caprabo(@DefaultValue("https://www.capraboacasa.com") String url) {
    }

    public record Aldi(@DefaultValue("https://www.aldi.es") String url) {
    }

    public record Eroski(@DefaultValue("https://supermercado.eroski.es") String url) {
    }

    public record Ahorramas(@DefaultValue("https://www.ahorramas.com") String url) {
    }

    public record Consum(@DefaultValue("https://tienda.consum.es") String url) {
    }

    public record Lidl(@DefaultValue("https://www.lidl.es") String url) {
    }

    public record Mercadona(@DefaultValue("https://tienda.mercadona.es/api") String url,
                            @DefaultValue("mad1") String almacen) {
    }
}
