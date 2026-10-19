package dev.pimon.listillo.gasolina.estaciones;

import dev.pimon.common.exception.AppException;
import dev.pimon.listillo.gasolina.estaciones.dto.BusquedaCercanasDto;
import dev.pimon.listillo.gasolina.estaciones.dto.BusquedaCercanasRequest;
import dev.pimon.listillo.gasolina.estaciones.dto.EstacionCercanaDto;
import dev.pimon.listillo.gasolina.estaciones.dto.PaginaDto;
import dev.pimon.listillo.gasolina.estaciones.dto.ResumenPreciosDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Busca estaciones alrededor de un punto. Aquí vive la lógica de la aplicación;
 * el controller solo traduce HTTP.
 */
@Service
public class BuscadorEstaciones {

    private static final int TAMANO_PAGINA_POR_DEFECTO = 20;

    private final CatalogoEstaciones catalogo;
    private final GasolinaProperties  props;

    public BuscadorEstaciones(CatalogoEstaciones catalogo, GasolinaProperties props) {
        this.catalogo = catalogo;
        this.props    = props;
    }

    public BusquedaCercanasDto cercanas(BusquedaCercanasRequest peticion) {
        exigirCatalogo();

        OrdenBusqueda orden = peticion.orden() == null ? OrdenBusqueda.DISTANCIA : peticion.orden();
        Carburante carburante = peticion.carburante();

        // Ordenar por precio sin decir de qué carburante no significa nada: es un error
        // del cliente, y decirlo es más útil que devolver un orden arbitrario.
        if (orden == OrdenBusqueda.PRECIO && carburante == null) {
            throw AppException.badRequest("Para ordenar por precio hay que indicar el carburante");
        }

        double radioKm = radioValido(peticion.radioKm());
        int pagina     = peticion.pagina() == null ? 0 : peticion.pagina();
        int tamano     = tamanoValido(peticion.tamano());

        double lat = peticion.latitud();
        double lon = peticion.longitud();


        // Primero la caja (cuatro comparaciones), luego el carburante y solo al final el
        // haversine sobre las pocas que sobreviven: así el cálculo caro se hace decenas
        // de veces en vez de once mil.
        Geo.CajaBusqueda caja = Geo.caja(lat, lon, radioKm);

        List<EstacionConDistancia> encontradas = catalogo.todas().stream()
                .filter(e -> caja.contiene(e.latitud(), e.longitud()))
                .filter(e -> carburante == null || e.vende(carburante))
                .map(e -> new EstacionConDistancia(e, Geo.distanciaKm(lat, lon, e.latitud(), e.longitud())))
                .filter(e -> e.distanciaKm() <= radioKm)
                .sorted(comparador(orden, carburante, peticion.idsFavoritos()))
                .toList();

        List<EstacionCercanaDto> dtos = encontradas.stream()
                .map(e -> EstacionMapper.aDto(e.estacion(), e.distanciaKm(), carburante))
                .toList();

        // El resumen sale de `encontradas` —la lista ENTERA— y no de la página: si se
        // calculara sobre los veinte que se enseñan, el número cambiaría al pasar de
        // página y una referencia que baila es peor que ninguna.
        return new BusquedaCercanasDto(
                PaginaDto.de(dtos, pagina, tamano),
                resumen(encontradas, carburante).orElse(null));
    }

    /**
     * Orden de los resultados: primero las favoritas del usuario y, dentro de cada grupo,
     * el criterio que haya pedido.
     *
     * Las favoritas van delante ANTES de paginar, y por eso este orden se hace aquí y no
     * en el navegador: reordenar en el cliente solo movería las de la página que se está
     * viendo, y una favorita que cayera en la página siete seguiría enterrada ahí.
     */
    private Comparator<EstacionConDistancia> comparador(
            OrdenBusqueda orden, Carburante carburante, Set<String> idsFavoritos) {

        Comparator<EstacionConDistancia> criterio = criterioPedido(orden, carburante);

        if (idsFavoritos.isEmpty()) {
            return criterio;
        }

        // Ojo al signo: se compara la NEGACIÓN de "es favorita" porque false ordena antes
        // que true, y así las favoritas quedan arriba.
        return Comparator.<EstacionConDistancia, Boolean>comparing(
                e -> !idsFavoritos.contains(e.estacion().id())).thenComparing(criterio);
    }

    /**
     * Cómo está el precio en lo que se acaba de encontrar: la media y la más barata.
     *
     * Es la referencia buena para decidir, y la razón es la misma por la que en el
     * historial se dejó de usar la media provincial: <b>lo que importa son las opciones
     * que uno tiene delante</b>, no un promedio de gasolineras a las que nunca iría. Aquí
     * el conjunto es literalmente lo que el usuario está viendo, con el radio que él ha
     * elegido.
     *
     * Vacío si no hay carburante seleccionado —cada estación vende varios a precios
     * distintos y promediarlos todos no significa nada— o si no se ha encontrado ninguna.
     */
    private Optional<ResumenPreciosDto> resumen(List<EstacionConDistancia> encontradas,
                                                Carburante carburante) {
        if (carburante == null || encontradas.isEmpty()) {
            return Optional.empty();
        }

        // El filtro por carburante de arriba garantiza que todas tienen precio.
        List<BigDecimal> precios = encontradas.stream()
                .map(e -> e.estacion().precios().get(carburante))
                .toList();

        BigDecimal medio = precios.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(precios.size()), 3, RoundingMode.HALF_UP);

        return Optional.of(new ResumenPreciosDto(
                medio,
                precios.stream().min(Comparator.naturalOrder()).orElseThrow(),
                precios.size()));
    }

    /**
     * Al ordenar por precio se desempata por distancia: entre dos gasolineras al mismo
     * precio, la de al lado es mejor que la de veinte kilómetros.
     */
    private Comparator<EstacionConDistancia> criterioPedido(OrdenBusqueda orden, Carburante carburante) {
        Comparator<EstacionConDistancia> porDistancia =
                Comparator.comparingDouble(EstacionConDistancia::distanciaKm);

        if (orden == OrdenBusqueda.DISTANCIA) {
            return porDistancia;
        }
        // El filtro por carburante garantiza que todas tienen precio, así que aquí no hay nulos.
        return Comparator.<EstacionConDistancia, BigDecimal>comparing(
                e -> e.estacion().precios().get(carburante)).thenComparing(porDistancia);
    }

    private double radioValido(Double solicitado) {
        double radio = solicitado == null ? props.busqueda().radioPorDefectoKm() : solicitado;
        // Se recorta en silencio en vez de dar error: pedir 200 km es una preferencia
        // razonable del usuario, no un fallo, y devolverle las de 50 km es lo que quería.
        return Math.min(radio, props.busqueda().radioMaximoKm());
    }

    private int tamanoValido(Integer solicitado) {
        int tamano = solicitado == null ? TAMANO_PAGINA_POR_DEFECTO : solicitado;
        return Math.min(tamano, props.busqueda().tamanoPaginaMaximo());
    }

    /**
     * Mientras no haya habido una primera descarga correcta no hay nada que buscar.
     * Es un 503 y no un 500: el fallo es temporal y reintentar tiene sentido.
     */
    private void exigirCatalogo() {
        if (!catalogo.disponible()) {
            throw new AppException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "CATALOGO_NO_DISPONIBLE",
                    "Todavía no se han podido cargar los datos del Ministerio. Inténtalo en unos minutos.");
        }
    }

    /** La estación y lo que dista del punto buscado, para no recalcular el haversine al ordenar. */
    private record EstacionConDistancia(Estacion estacion, double distanciaKm) {
    }
}
