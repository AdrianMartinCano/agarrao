package dev.pimon.listillo;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.SpringApplication;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Arranca el back en local SIN Docker ni Postgres instalado: levanta un Postgres embebido
 * (el mismo de los tests) y la aplicación encima.
 *
 * <pre>mvn spring-boot:test-run -Dspring-boot.run.main-class=dev.pimon.listillo.ListilloLocal</pre>
 *
 * Los datos se guardan en {@code back/.local-db/} (fuera de git): se conservan entre
 * arranques. Borrar esa carpeta es empezar de cero.
 *
 * Con la base vacía crea el usuario de prueba {@link #USUARIO_PRUEBA} (rol ADMIN, para
 * poder tocar también el panel de versiones). En cada arranque le rehace el hogar «Casa de
 * ejemplo» con siete meses de tickets inventados ({@link DatosDeEjemplo}). Vive en el código
 * de test: nunca llega a la imagen de producción.
 */
public final class ListilloLocal {

    static final String USUARIO_PRUEBA = "prueba@example.com";
    static final String CLAVE_PRUEBA = "Listillo1234!";
    private static final int PUERTO_POSTGRES = 54320;

    private ListilloLocal() {
    }

    public static void main(String[] args) throws IOException {
        EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setDataDirectory(Path.of(".local-db"))
                .setCleanDataDirectory(false)
                .setPort(PUERTO_POSTGRES)
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                postgres.close();
            } catch (IOException e) {
                // Al apagar da igual: el proceso termina de todas formas.
            }
        }));

        System.setProperty("spring.datasource.url", postgres.getJdbcUrl("postgres", "postgres"));
        System.setProperty("spring.datasource.username", "postgres");
        System.setProperty("spring.datasource.password", "postgres");
        System.setProperty("pimon.users.create-default-admin", "true");
        System.setProperty("pimon.users.admin-email", USUARIO_PRUEBA);
        System.setProperty("pimon.users.admin-password", CLAVE_PRUEBA);
        System.setProperty("pimon.users.admin-name", "Prueba");
        // Para crear más personas de prueba con POST /api/auth/register (cerrado en producción).
        System.setProperty("listillo.registro-con-contrasena", "true");
        // Precios de las webs de los súper: en local sí (uso privado). Se importan al arrancar
        // si tienen más de un día. Ver tienda/ImportadorPrecios.
        System.setProperty("listillo.precios-web.activo", "true");

        dev.pimon.listillo.gasolina.estaciones.minetur.TlsDelMinisterio.preparar();
        var contexto = SpringApplication.run(ListilloApplication.class, args);
        // Un hogar con meses de tickets inventados para ver Precios y Gasto con datos.
        DatosDeEjemplo.rehacer(contexto, USUARIO_PRUEBA);
    }
}
