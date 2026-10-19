package dev.pimon.listillo.reparto;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.listillo.reparto.dto.RepartoDto;
import dev.pimon.security.annotation.CurrentUser;
import dev.pimon.security.model.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** GET /api/hogares/{h}/lista/reparto   lo pendiente de la lista, repartido por el súper más barato. */
@RestController
@RequiredArgsConstructor
public class RepartoController {

    private final RepartoService servicio;

    @GetMapping("/api/hogares/{hogarId}/lista/reparto")
    public ApiResponse<RepartoDto> repartir(@CurrentUser AuthUser yo, @PathVariable UUID hogarId) {
        return ApiResponse.ok(servicio.repartir(hogarId, yo.id()));
    }
}
