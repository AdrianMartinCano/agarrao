package dev.pimon.listillo.tienda;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.listillo.tienda.dto.BusquedaDto;
import dev.pimon.security.annotation.CurrentUser;
import dev.pimon.security.model.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * GET /api/hogares/{h}/precios/buscar?q=fregona                   lo que se llama así en cada súper, el más barato primero.
 * GET /api/hogares/{h}/precios/en-cada-super?cadena=X&id=Y         ese producto en cada súper.
 */
@RestController
@RequiredArgsConstructor
public class BuscadorController {

    private final BuscadorService servicio;

    @GetMapping("/api/hogares/{hogarId}/precios/buscar")
    public ApiResponse<BusquedaDto> buscar(@CurrentUser AuthUser yo, @PathVariable UUID hogarId,
                                           @RequestParam("q") String texto) {
        return ApiResponse.ok(servicio.buscar(hogarId, yo.id(), texto));
    }

    @GetMapping("/api/hogares/{hogarId}/precios/en-cada-super")
    public ApiResponse<dev.pimon.listillo.tienda.dto.EnCadaSuperDto> enCadaSuper(
            @CurrentUser AuthUser yo, @PathVariable UUID hogarId,
            @RequestParam dev.pimon.listillo.ticket.Cadena cadena, @RequestParam("id") String idExterno) {
        return ApiResponse.ok(servicio.enCadaSuper(hogarId, yo.id(), cadena, idExterno));
    }
}
