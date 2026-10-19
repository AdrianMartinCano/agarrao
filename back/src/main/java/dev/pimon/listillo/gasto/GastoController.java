package dev.pimon.listillo.gasto;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.listillo.gasto.dto.GastoDto;
import dev.pimon.security.annotation.CurrentUser;
import dev.pimon.security.model.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** GET /api/hogares/{h}/gasto?mes=2026-10   el gasto del mes (sin mes: el actual) y los cinco anteriores. */
@RestController
@RequestMapping("/api/hogares/{hogarId}/gasto")
@RequiredArgsConstructor
public class GastoController {

    private final GastoService servicio;
    private final GastosFijosAlDia alDia;

    @GetMapping
    public ApiResponse<GastoDto> resumen(@CurrentUser AuthUser yo, @PathVariable UUID hogarId,
                                         @RequestParam(required = false) String mes) {
        alDia.poner(hogarId, yo.id());
        return ApiResponse.ok(servicio.resumen(hogarId, yo.id(), mes));
    }
}
