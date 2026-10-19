package dev.pimon.listillo.gasolina.favoritos;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.listillo.gasolina.estaciones.Carburante;
import dev.pimon.listillo.gasolina.estaciones.dto.EstacionCercanaDto;
import dev.pimon.security.annotation.CurrentUser;
import dev.pimon.security.model.AuthUser;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Favoritos del usuario autenticado.
 *
 * Al no estar en {@code pimon.security.public-paths}, todo lo de aquí exige un JWT
 * válido. El usuario llega en {@code @CurrentUser}, sacado del token: nunca se acepta
 * un id de usuario por parámetro, que sería la forma más fácil de leer los favoritos
 * de otra persona.
 */
@Validated
@RestController
@RequestMapping("/api/favoritos")
public class FavoritosController {

    private final FavoritosService servicio;

    public FavoritosController(FavoritosService servicio) {
        this.servicio = servicio;
    }

    /** Favoritos con precios actuales y, si se manda ubicación, con su distancia. */
    @GetMapping
    public ApiResponse<List<EstacionCercanaDto>> listar(
            @CurrentUser AuthUser usuario,

            @RequestParam(required = false)
            @DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90")
            @DecimalMax(value = "90",  message = "La latitud debe estar entre -90 y 90")
            Double latitud,

            @RequestParam(required = false)
            @DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180")
            @DecimalMax(value = "180",  message = "La longitud debe estar entre -180 y 180")
            Double longitud,

            @RequestParam(required = false) Carburante carburante) {

        return ApiResponse.ok(servicio.listar(usuario.id(), latitud, longitud, carburante));
    }

    /** Añade una estación a favoritos. Repetir la llamada no da error ni duplica. */
    @PostMapping("/{estacionId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> anadir(@CurrentUser AuthUser usuario, @PathVariable String estacionId) {
        servicio.anadir(usuario.id(), estacionId);
        return ApiResponse.noContent("Añadida a favoritos");
    }

    @DeleteMapping("/{estacionId}")
    public ApiResponse<Void> eliminar(@CurrentUser AuthUser usuario, @PathVariable String estacionId) {
        servicio.eliminar(usuario.id(), estacionId);
        return ApiResponse.noContent("Quitada de favoritos");
    }
}
