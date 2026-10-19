package dev.pimon.listillo.hogar;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.listillo.hogar.dto.HogarDto;
import dev.pimon.listillo.hogar.dto.HogarResumenDto;
import dev.pimon.listillo.hogar.dto.InvitacionDto;
import dev.pimon.listillo.hogar.dto.NombreHogarRequest;
import dev.pimon.listillo.hogar.dto.UnirseRequest;
import dev.pimon.security.annotation.CurrentUser;
import dev.pimon.security.model.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * GET    /api/hogares                          mis hogares
 * POST   /api/hogares                          crear uno (quien lo crea lo lleva)
 * GET    /api/hogares/invitacion/{codigo}      a qué hogar lleva un código (antes de entrar)
 * POST   /api/hogares/unirse                   entrar con un código
 * GET    /api/hogares/{id}                     detalle con miembros y código
 * PATCH  /api/hogares/{id}                     cambiar el nombre
 * POST   /api/hogares/{id}/codigo              código nuevo (propietario)
 * DELETE /api/hogares/{id}/miembros/yo         salir
 * DELETE /api/hogares/{id}/miembros/{usuario}  sacar a alguien (propietario)
 */
@RestController
@RequestMapping("/api/hogares")
@RequiredArgsConstructor
public class HogarController {

    private final HogarService servicio;

    @GetMapping
    public ApiResponse<List<HogarResumenDto>> misHogares(@CurrentUser AuthUser yo) {
        return ApiResponse.ok(servicio.misHogares(yo.id()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HogarDto> crear(@CurrentUser AuthUser yo,
                                       @Valid @RequestBody NombreHogarRequest peticion) {
        return ApiResponse.ok(servicio.crear(yo.id(), peticion.nombre()));
    }

    @GetMapping("/invitacion/{codigo}")
    public ApiResponse<InvitacionDto> invitacion(@CurrentUser AuthUser yo, @PathVariable String codigo) {
        return ApiResponse.ok(servicio.invitacion(codigo, yo.id()));
    }

    @PostMapping("/unirse")
    public ApiResponse<HogarDto> unirse(@CurrentUser AuthUser yo, @Valid @RequestBody UnirseRequest peticion) {
        return ApiResponse.ok(servicio.unirse(yo.id(), peticion.codigo()));
    }

    @GetMapping("/{id}")
    public ApiResponse<HogarDto> detalle(@CurrentUser AuthUser yo, @PathVariable UUID id) {
        return ApiResponse.ok(servicio.detalle(id, yo.id()));
    }

    @PatchMapping("/{id}")
    public ApiResponse<HogarDto> renombrar(@CurrentUser AuthUser yo, @PathVariable UUID id,
                                           @Valid @RequestBody NombreHogarRequest peticion) {
        return ApiResponse.ok(servicio.renombrar(id, yo.id(), peticion.nombre()));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}/cadenas")
    public ApiResponse<HogarDto> elegirCadenas(@CurrentUser AuthUser yo, @PathVariable UUID id,
                                               @Valid @RequestBody dev.pimon.listillo.hogar.dto.CadenasRequest peticion) {
        return ApiResponse.ok(servicio.elegirCadenas(id, yo.id(), peticion.cadenas()));
    }

    @PostMapping("/{id}/codigo")
    public ApiResponse<HogarDto> nuevoCodigo(@CurrentUser AuthUser yo, @PathVariable UUID id) {
        return ApiResponse.ok(servicio.nuevoCodigo(id, yo.id()));
    }

    @DeleteMapping("/{id}/miembros/yo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void salir(@CurrentUser AuthUser yo, @PathVariable UUID id) {
        servicio.salir(id, yo.id());
    }

    @DeleteMapping("/{id}/miembros/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void echar(@CurrentUser AuthUser yo, @PathVariable UUID id, @PathVariable String usuarioId) {
        servicio.echar(id, yo.id(), usuarioId);
    }
}
