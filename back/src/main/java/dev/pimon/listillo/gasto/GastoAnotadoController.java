package dev.pimon.listillo.gasto;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.listillo.gasto.dto.GastoAnotadoDto;
import dev.pimon.listillo.gasto.dto.GastoFijoDto;
import dev.pimon.listillo.gasto.dto.GastoRequest;
import dev.pimon.security.annotation.CurrentUser;
import dev.pimon.security.model.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Los gastos que no son tickets del súper.
 *
 * GET    /api/hogares/{h}/gastos?mes=2026-10   los anotados del mes (sin mes: el actual)
 * POST   /api/hogares/{h}/gastos               anotar (con cadaMes, además se apunta solo cada mes)
 * GET    /api/hogares/{h}/gastos/{id}          uno
 * PUT    /api/hogares/{h}/gastos/{id}          cambiar
 * DELETE /api/hogares/{h}/gastos/{id}
 * GET    /api/hogares/{h}/gastos/fijos         los que se apuntan solos cada mes
 * DELETE /api/hogares/{h}/gastos/fijos/{id}    que deje de apuntarse (lo ya apuntado se queda)
 */
@RestController
@RequestMapping("/api/hogares/{hogarId}/gastos")
@RequiredArgsConstructor
public class GastoAnotadoController {

    private final GastoAnotadoService servicio;
    private final GastosFijosAlDia alDia;

    @GetMapping
    public ApiResponse<List<GastoAnotadoDto>> delMes(@CurrentUser AuthUser yo, @PathVariable UUID hogarId,
                                                     @RequestParam(required = false) String mes) {
        alDia.poner(hogarId, yo.id());
        return ApiResponse.ok(servicio.delMes(hogarId, yo.id(), GastoService.mes(mes)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GastoAnotadoDto> anotar(@CurrentUser AuthUser yo, @PathVariable UUID hogarId,
                                               @Valid @RequestBody GastoRequest peticion) {
        return ApiResponse.ok(servicio.anotar(hogarId, yo.id(), peticion));
    }

    @GetMapping("/{gastoId}")
    public ApiResponse<GastoAnotadoDto> uno(@CurrentUser AuthUser yo, @PathVariable UUID hogarId,
                                            @PathVariable UUID gastoId) {
        return ApiResponse.ok(servicio.uno(hogarId, gastoId, yo.id()));
    }

    @PutMapping("/{gastoId}")
    public ApiResponse<GastoAnotadoDto> cambiar(@CurrentUser AuthUser yo, @PathVariable UUID hogarId,
                                                @PathVariable UUID gastoId, @Valid @RequestBody GastoRequest peticion) {
        return ApiResponse.ok(servicio.cambiar(hogarId, gastoId, yo.id(), peticion));
    }

    @DeleteMapping("/{gastoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrar(@CurrentUser AuthUser yo, @PathVariable UUID hogarId, @PathVariable UUID gastoId) {
        servicio.borrar(hogarId, gastoId, yo.id());
    }

    @GetMapping("/fijos")
    public ApiResponse<List<GastoFijoDto>> fijos(@CurrentUser AuthUser yo, @PathVariable UUID hogarId) {
        return ApiResponse.ok(servicio.fijos(hogarId, yo.id()));
    }

    @DeleteMapping("/fijos/{fijoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarFijo(@CurrentUser AuthUser yo, @PathVariable UUID hogarId, @PathVariable UUID fijoId) {
        servicio.quitarFijo(hogarId, fijoId, yo.id());
    }
}
