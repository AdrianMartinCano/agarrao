package dev.pimon.listillo.gasolina.estaciones;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.common.exception.AppException;
import dev.pimon.listillo.gasolina.estaciones.dto.BusquedaCercanasDto;
import dev.pimon.listillo.gasolina.estaciones.dto.BusquedaCercanasRequest;
import dev.pimon.listillo.gasolina.estaciones.dto.CarburanteDto;
import dev.pimon.listillo.gasolina.estaciones.dto.EstacionCercanaDto;
import dev.pimon.listillo.gasolina.estaciones.dto.EstadoCatalogoDto;
import dev.pimon.listillo.gasolina.estaciones.dto.LugarDto;
import dev.pimon.listillo.gasolina.estaciones.dto.PaginaDto;
import dev.pimon.listillo.gasolina.favoritos.FavoritosService;
import dev.pimon.listillo.gasolina.seguridad.UsuarioActual;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * Consulta pública de gasolineras. No requiere estar registrado: los datos son
 * públicos y obligar a crear cuenta para mirar precios no tendría sentido.
 *
 * {@code @Validated} hace que los @RequestParam se validen de verdad; los fallos
 * salen como ConstraintViolationException y el GlobalExceptionHandler de la librería
 * los convierte en un 400 con el detalle por campo.
 */
@Validated
@RestController
@RequestMapping("/api/estaciones")
public class EstacionesController {

    private final BuscadorEstaciones buscador;
    private final CatalogoEstaciones catalogo;
    private final FavoritosService   favoritos;

    public EstacionesController(BuscadorEstaciones buscador, CatalogoEstaciones catalogo,
                                FavoritosService favoritos) {
        this.buscador  = buscador;
        this.catalogo  = catalogo;
        this.favoritos = favoritos;
    }

    /**
     * Estaciones dentro de un radio, ordenadas por distancia o por precio.
     *
     * Ejemplo: /api/estaciones/cercanas?latitud=40.41&longitud=-3.70&radioKm=5
     *          &carburante=GASOLINA_95_E5&orden=PRECIO
     *
     * Los rangos de latitud y longitud se validan aquí: sin ellos, unas coordenadas
     * cambiadas de sitio devolverían una lista vacía sin explicar por qué.
     */
    @GetMapping("/cercanas")
    public ApiResponse<BusquedaCercanasDto> cercanas(

            @RequestParam
            @DecimalMin(value = "-90",  message = "La latitud debe estar entre -90 y 90")
            @DecimalMax(value = "90",   message = "La latitud debe estar entre -90 y 90")
            double latitud,

            @RequestParam
            @DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180")
            @DecimalMax(value = "180",  message = "La longitud debe estar entre -180 y 180")
            double longitud,

            @RequestParam(required = false)
            @Positive(message = "El radio debe ser mayor que cero")
            Double radioKm,

            @RequestParam(required = false) Carburante carburante,
            @RequestParam(required = false) OrdenBusqueda orden,

            @RequestParam(required = false)
            @Min(value = 0, message = "La página no puede ser negativa")
            Integer pagina,

            @RequestParam(required = false)
            @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
            Integer tamano) {

        // La búsqueda es pública, pero si quien la hace ha entrado con su cuenta, sus
        // favoritas van las primeras. Los ids salen del token, nunca de un parámetro:
        // así nadie puede pedir "ordéname por los favoritos de otro".
        Set<String> idsFavoritos = UsuarioActual.opcional()
                .map(usuario -> favoritos.idsDe(usuario.id()))
                .orElseGet(Set::of);

        var peticion = new BusquedaCercanasRequest(
                latitud, longitud, radioKm, carburante, orden, pagina, tamano, idsFavoritos);

        return ApiResponse.ok(buscador.cercanas(peticion));
    }

    /**
     * Detalle de una estación por su IDEESS.
     *
     * Convive con /carburantes y /estado sin pisarlos: ante dos patrones que encajan,
     * Spring elige siempre el literal antes que el que lleva variable.
     */
    @GetMapping("/{id}")
    public ApiResponse<EstacionCercanaDto> detalle(@PathVariable String id) {
        return catalogo.porId(id)
                .map(estacion -> ApiResponse.ok(EstacionMapper.aDto(estacion, null, null)))
                .orElseThrow(() -> AppException.notFound("No hay ninguna estación con id " + id));
    }

    /**
     * Carburantes que se ofrecen en el buscador, con su nombre para mostrar.
     * El front pinta el desplegable con esto en vez de llevar la lista duplicada.
     *
     * Devuelve solo los habituales (gasolina 95 y 98, diésel, GLP y AdBlue). Los otros
     * dieciocho que publica el Ministerio siguen leyéndose y guardándose para cada
     * estación, y la API los sigue aceptando en {@code /cercanas}; simplemente no se
     * ofrecen aquí, porque no los reposta casi nadie y enterraban a los que sí.
     */
    @GetMapping("/carburantes")
    public ApiResponse<List<CarburanteDto>> carburantes() {
        return ApiResponse.ok(Carburante.habituales().stream()
                .map(c -> new CarburanteDto(c.name(), c.etiqueta()))
                .toList());
    }

    /**
     * Municipios y códigos postales que encajan con lo escrito.
     *
     * Es lo que permite buscar sin dar la ubicación: se escribe "Getafe" o "28001" y se
     * busca desde ahí. Los lugares salen de las propias estaciones del Ministerio, así
     * que no hay ningún servicio de geocodificación detrás (ver {@link Lugar}).
     */
    @GetMapping("/lugares")
    public ApiResponse<List<LugarDto>> lugares(@RequestParam String q) {
        List<LugarDto> sugerencias = catalogo.lugares().buscar(q).stream()
                .map(lugar -> new LugarDto(
                        lugar.nombre(),
                        lugar.detalle(),
                        lugar.tipo().name(),
                        lugar.latitud(),
                        lugar.longitud(),
                        lugar.estaciones()))
                .toList();

        return ApiResponse.ok(sugerencias);
    }

    /**
     * Estado del catálogo. Sirve para que el front avise de que los precios son de
     * hace un rato si el Ministerio lleva tiempo sin responder.
     */
    @GetMapping("/estado")
    public ApiResponse<EstadoCatalogoDto> estado() {
        return ApiResponse.ok(new EstadoCatalogoDto(
                catalogo.disponible(),
                catalogo.todas().size(),
                catalogo.ultimaActualizacion().orElse(null)
        ));
    }
}
