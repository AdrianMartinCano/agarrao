package dev.pimon.listillo;

import dev.pimon.listillo.gasolina.estaciones.minetur.TlsDelMinisterio;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * API de Listillo.
 *
 * Los módulos spring-* (common, security, users, google) se registran solos con su
 * propia @AutoConfiguration: no hace falta @EntityScan ni @ComponentScan.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ListilloApplication {

    public static void main(String[] args) {
        TlsDelMinisterio.preparar();
        SpringApplication.run(ListilloApplication.class, args);
    }
}
