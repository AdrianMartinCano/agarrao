package dev.pimon.listillo;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Base de los tests con la aplicación entera: UN Postgres embebido para toda la ejecución
 * y el mismo contexto de Spring para todas las clases que heredan de aquí (Spring lo
 * reutiliza porque la configuración es idéntica). Se apaga solo al acabar la JVM.
 *
 * Como la base se comparte, cada test crea sus propios datos (usuarios con correo único)
 * y no cuenta con que esté vacía.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class ConPostgresEmbebido {

    private static final EmbeddedPostgres POSTGRES = arrancar();

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        // Sin salir a internet en los tests: Open Prices se prueba con un doble.
        registry.add("listillo.open-prices.activo", () -> "false");
        registry.add("listillo.gasolina.minetur.activo", () -> "false");
    }

    private static EmbeddedPostgres arrancar() {
        try {
            EmbeddedPostgres postgres = EmbeddedPostgres.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    postgres.close();
                } catch (IOException e) {
                    // La JVM se está cerrando: no hay nada más que hacer.
                }
            }));
            return postgres;
        } catch (IOException e) {
            throw new UncheckedIOException("No arranca el Postgres embebido", e);
        }
    }
}
