package dev.pimon.listillo.tienda;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** La importación nocturna de precios. El interruptor lo mira {@link ImportadorPrecios}. */
@Configuration
@EnableScheduling
class PreciosWebConfig {
}
