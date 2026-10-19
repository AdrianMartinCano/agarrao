package dev.pimon.listillo.gasolina.estaciones.minetur;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Confianza TLS para hablar con la sede del Ministerio.
 *
 * EL PROBLEMA. El certificado de sedeaplicaciones.minetur.gob.es lo firma una CA española
 * (hasta sep 2026, "Autoridad de Certificacion Firmaprofesional") que Windows sí trae de
 * serie pero que NO está en el almacén de confianza del JDK. Resultado: el navegador y
 * PowerShell entran sin problema, y Java falla con un
 * "PKIX path building failed: unable to find valid certification path", que no dice en
 * ningún momento qué CA es la que falta.
 *
 * LA SOLUCIÓN. Se confía en las CA de siempre del JDK MÁS las que se llevan en el propio
 * repositorio ({@code resources/certs/*.pem}, todas las que haya). Antes el almacén tenía
 * solo la de Firmaprofesional, y cuando el Ministerio renovó su certificado (sep 2026) la
 * descarga dejó de funcionar de un día para otro. Así:
 *
 *   - si el Ministerio pasa a una CA de las habituales, funciona sin tocar nada;
 *   - si pasa a otra española que el JDK no trae, basta con dejar su .pem en certs/;
 *   - y si vuelve a fallar, el log dice qué cadena presenta el servidor (quién firma cada
 *     certificado), que es justo lo que hace falta para saber qué .pem añadir.
 *
 * Frente a las alternativas:
 *   - importar la CA a mano con keytool en cada máquina: funciona hasta que alguien clona
 *     el repo o se reconstruye la imagen, y falla sin explicación;
 *   - {@code -Djavax.net.ssl.trustStoreType=WINDOWS-ROOT}: solo sirve en Windows, no en
 *     el contenedor de producción;
 *   - desactivar la validación del certificado: eso no es arreglarlo, es apagar la única
 *     comprobación que impide que alguien se haga pasar por el Ministerio.
 *
 * Solo lo usa este cliente: ninguna otra llamada de la aplicación se ve afectada.
 */
@Slf4j
public final class MineturTruststore {

    private static final String CERTIFICADOS = "classpath*:certs/*.pem";

    private MineturTruststore() {
    }

    /**
     * @return contexto SSL que confía en las CA del JDK y en las de {@code certs/}
     * @throws IllegalStateException si no hay ningún .pem en certs/ o no se puede leer: es
     *         mejor enterarse al arrancar que con un error de red confuso media hora después
     */
    public static SSLContext sslContext() {
        try {
            KeyStore almacen = KeyStore.getInstance(KeyStore.getDefaultType());
            almacen.load(null, null);

            // Las del JDK, las de siempre.
            int n = 0;
            for (X509Certificate ca : delJdk().getAcceptedIssuers()) {
                almacen.setCertificateEntry("jdk-" + n++, ca);
            }
            // Y las que llevamos nosotros.
            List<X509Certificate> propias = propias();
            for (int i = 0; i < propias.size(); i++) {
                almacen.setCertificateEntry("propia-" + i, propias.get(i));
            }

            TrustManagerFactory fabrica = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            fabrica.init(almacen);
            X509ExtendedTrustManager base = Arrays.stream(fabrica.getTrustManagers())
                    .filter(X509ExtendedTrustManager.class::isInstance)
                    .map(X509ExtendedTrustManager.class::cast)
                    .findFirst()
                    .orElseThrow();

            SSLContext contexto = SSLContext.getInstance("TLS");
            contexto.init(null, new TrustManager[]{new QueDiceLaCadena(base)}, null);

            log.debug("Confianza TLS del Ministerio: {} CA del JDK y {} propias ({})", n, propias.size(),
                    propias.stream().map(c -> c.getSubjectX500Principal().getName()).collect(Collectors.joining("; ")));
            return contexto;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo preparar la confianza TLS con el Ministerio (¿falta certs/*.pem?)", e);
        }
    }

    private static X509TrustManager delJdk() throws Exception {
        TrustManagerFactory fabrica = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        fabrica.init((KeyStore) null);
        return Arrays.stream(fabrica.getTrustManagers())
                .filter(X509TrustManager.class::isInstance)
                .map(X509TrustManager.class::cast)
                .findFirst()
                .orElseThrow();
    }

    /** Todos los certificados de todos los .pem de certs/ (un .pem puede traer varios). */
    static List<X509Certificate> propias() throws Exception {
        Resource[] ficheros = new PathMatchingResourcePatternResolver().getResources(CERTIFICADOS);
        CertificateFactory fabrica = CertificateFactory.getInstance("X.509");
        List<X509Certificate> certificados = new ArrayList<>();
        for (Resource fichero : ficheros) {
            try (InputStream pem = fichero.getInputStream()) {
                for (Certificate c : fabrica.generateCertificates(pem)) {
                    certificados.add((X509Certificate) c);
                }
            }
        }
        if (certificados.isEmpty()) {
            throw new IllegalStateException("No hay ningún certificado en certs/*.pem");
        }
        return certificados;
    }

    /** "CN=… (firmado por CN=…, caduca 2027-…)" por cada certificado, del servidor a la raíz. */
    static String describir(X509Certificate[] cadena) {
        if (cadena == null || cadena.length == 0) {
            return "(el servidor no ha enviado ningún certificado)";
        }
        return Arrays.stream(cadena)
                .map(c -> "[" + c.getSubjectX500Principal().getName() + "] firmado por ["
                        + c.getIssuerX500Principal().getName() + "], caduca " + c.getNotAfter().toInstant())
                .collect(Collectors.joining(" -> "));
    }

    /**
     * El de siempre, pero si no se fía del servidor, dice en el log qué cadena ha presentado.
     * El error de Java ("unable to find valid certification path") no lo dice, y sin eso no
     * hay forma de saber qué CA falta.
     */
    private static final class QueDiceLaCadena extends X509ExtendedTrustManager {

        private final X509ExtendedTrustManager base;

        QueDiceLaCadena(X509ExtendedTrustManager base) {
            this.base = base;
        }

        private static CertificateException contar(X509Certificate[] cadena, CertificateException e) {
            log.error("El Ministerio presenta un certificado del que no nos fiamos. Su cadena: {}. "
                    + "Si la última CA no es de las habituales, añade su certificado raíz (.pem) a "
                    + "src/main/resources/certs/.", describir(cadena));
            return e;
        }

        @Override
        public void checkServerTrusted(X509Certificate[] cadena, String tipo, Socket socket) throws CertificateException {
            try {
                base.checkServerTrusted(cadena, tipo, socket);
            } catch (CertificateException e) {
                throw contar(cadena, e);
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] cadena, String tipo, SSLEngine motor) throws CertificateException {
            try {
                base.checkServerTrusted(cadena, tipo, motor);
            } catch (CertificateException e) {
                throw contar(cadena, e);
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] cadena, String tipo) throws CertificateException {
            try {
                base.checkServerTrusted(cadena, tipo);
            } catch (CertificateException e) {
                throw contar(cadena, e);
            }
        }

        @Override
        public void checkClientTrusted(X509Certificate[] cadena, String tipo, Socket socket) throws CertificateException {
            base.checkClientTrusted(cadena, tipo, socket);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] cadena, String tipo, SSLEngine motor) throws CertificateException {
            base.checkClientTrusted(cadena, tipo, motor);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] cadena, String tipo) throws CertificateException {
            base.checkClientTrusted(cadena, tipo);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return base.getAcceptedIssuers();
        }
    }
}
