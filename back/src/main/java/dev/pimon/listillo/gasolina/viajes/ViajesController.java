package dev.pimon.listillo.gasolina.viajes;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.listillo.gasolina.estaciones.Carburante;
import dev.pimon.listillo.gasolina.viajes.ViajeDtos.ViajeDto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Viajes: gasolineras baratas de camino. Público, como el buscador (cuelga de
 * {@code /api/estaciones/**}). «/ruta» es literal, así que gana a {@code /{id}}.
 *
 * Ejemplo: /api/estaciones/ruta?origenLatitud=40.41&origenLongitud=-3.70
 *          &destinoLatitud=39.47&destinoLongitud=-0.37&carburante=GASOLINA_95_E5&desvioKm=2
 */
@Validated
@RestController
@RequestMapping("/api/estaciones")
public class ViajesController {

    private final GasolinerasEnRuta gasolineras;

    public ViajesController(GasolinerasEnRuta gasolineras) {
        this.gasolineras = gasolineras;
    }

    @GetMapping("/ruta")
    public ApiResponse<ViajeDto> ruta(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double origenLatitud,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double origenLongitud,
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double destinoLatitud,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double destinoLongitud,
            @RequestParam Carburante carburante,
            @RequestParam(required = false) @Positive(message = "El desvío debe ser mayor que cero") Double desvioKm) {
        return ApiResponse.ok(gasolineras.buscar(
                origenLatitud, origenLongitud, destinoLatitud, destinoLongitud, carburante, desvioKm));
    }
}
