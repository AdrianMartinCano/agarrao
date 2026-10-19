package dev.pimon.listillo.gasolina.estaciones.minetur;

import dev.pimon.listillo.gasolina.estaciones.Estacion;
import dev.pimon.listillo.gasolina.estaciones.GasolinaProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.net.ssl.SSLParameters;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Objects;

/**
 * Descarga el listado de estaciones del Ministerio.
 *
 * Se usa RestClient y no WebClient a posta: WebClient limita el cuerpo a 256 KB por
 * defecto y esta respuesta pesa ~12 MB, así que fallaría con un
 * "DataBufferLimitException" que no se parece en nada al problema real.
 */
@Slf4j
@Component
public class MineturClient {

    private final RestClient restClient;
    private final String url;

    public MineturClient(GasolinaProperties props) {
        this.url = props.minetur().url();

        /*
         * TLS DEL MINISTERIO. Dos problemas distintos, y hacen falta las dos soluciones.
         *
         * 1) El servidor NO habla TLS 1.3: en cuanto el cliente lo propone, corta la
         *    conexión de golpe en vez de contestar que no lo soporta. Y como el corte es
         *    abrupto, HttpClient no reintenta con 1.2 solo; se queda con:
         *
         *      SSLHandshakeException: Remote host terminated the handshake
         *      Caused by: java.net.SocketException: Connection reset
         *
         *    De ahí que se fije TLSv1.2 a mano. Medido contra el servidor: 1.2 conecta,
         *    1.3 resetea.
         *
         *    Ojo: NO se tocan las cipher suites, y fijar una a mano no arreglaría nada.
         *    El servidor solo ofrece TLS_RSA_WITH_AES_256_GCM_SHA384, que Java desactiva
         *    globalmente desde 21.0.12; mientras esté en jdk.tls.disabledAlgorithms, da
         *    igual pedirla por SSLParameters. Eso se resuelve en TlsDelMinisterio, que
         *    la rehabilita al arrancar. Aquí basta con dejar que negocien.
         *
         * 2) Su certificado lo firma una CA que el JDK no lleva: ver MineturTruststore.
         *    Sin esa parte, superado el handshake vendría un "PKIX path building failed".
         */
        SSLParameters sslParameters = new SSLParameters();
        sslParameters.setProtocols(new String[]{"TLSv1.2"});

        HttpClient httpClient = HttpClient.newBuilder()
                // HTTP/1.1 explícito: sin esto se intenta HTTP/2 por ALPN, otra cosa más
                // que negociar con un servidor que ya viene justo de protocolos.
                .version(HttpClient.Version.HTTP_1_1)
                .sslContext(MineturTruststore.sslContext())
                .sslParameters(sslParameters)
                .connectTimeout(props.minetur().timeout())
                .build();

        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(props.minetur().timeout());

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    /**
     * Pide todas las estaciones y las devuelve ya convertidas a dominio.
     *
     * @return la lista completa; nunca null
     * @throws MineturNoDisponibleException si el servicio falla, tarda de más o responde
     *         algo que no es un listado correcto
     */
    public List<Estacion> descargarEstaciones() {
        log.info("Descargando estaciones del Ministerio: {}", url);
        long inicio = System.currentTimeMillis();

        MineturRespuesta respuesta;

        try {
            respuesta = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(MineturRespuesta.class);
        } catch (RestClientException e) {
            throw new MineturNoDisponibleException(
                    "No se pudo consultar el servicio del Ministerio",
                    e
            );
        }

        /*
         * El servicio responde 200 con la lista vacía cuando algo va mal por dentro,
         * así que un HTTP correcto no basta para dar la descarga por buena.
         */
        if (respuesta == null
                || !respuesta.correcta()
                || respuesta.estaciones() == null) {

            throw new MineturNoDisponibleException(
                    "El Ministerio respondió sin datos utilizables (resultado: %s)"
                            .formatted(
                                    respuesta == null
                                            ? "sin cuerpo"
                                            : respuesta.resultadoConsulta()
                            )
            );
        }

        List<Estacion> estaciones = respuesta.estaciones().stream()
                .map(MineturEstacion::aEstacion)
                .flatMap(java.util.Optional::stream)
                .toList();

        long descartadas =
                respuesta.estaciones().size() - estaciones.size();

        log.info(
                "Descargadas {} estaciones en {} ms " +
                        "(fecha del origen: {}, descartadas por datos incompletos: {})",
                estaciones.size(),
                System.currentTimeMillis() - inicio,
                respuesta.fecha(),
                descartadas
        );

        if (estaciones.isEmpty()) {
            throw new MineturNoDisponibleException(
                    "El Ministerio devolvió un listado sin ninguna estación usable"
            );
        }

        return estaciones;
    }

    /**
     * Fallo al hablar con el Ministerio.
     * Es temporal por naturaleza: se reintenta, no se propaga al usuario.
     */
    public static class MineturNoDisponibleException extends RuntimeException {

        public MineturNoDisponibleException(String mensaje) {
            super(Objects.requireNonNull(mensaje));
        }

        public MineturNoDisponibleException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }
}