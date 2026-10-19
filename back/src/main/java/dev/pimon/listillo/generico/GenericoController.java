package dev.pimon.listillo.generico;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.listillo.generico.dto.ClasificacionLineaDto;
import dev.pimon.listillo.generico.dto.ClasificarRequest;
import dev.pimon.listillo.generico.dto.ClasificarWebRequest;
import dev.pimon.listillo.generico.dto.GenericoDetalleDto;
import dev.pimon.listillo.generico.dto.GenericoDto;
import dev.pimon.listillo.generico.dto.GenericoResumenDto;
import dev.pimon.listillo.generico.dto.NuevoGenericoRequest;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * GET    /api/hogares/{h}/genericos                  catálogo con el mejor €/unidad de cada uno
 * GET    /api/hogares/{h}/genericos/{id}             comparación por tienda y producto
 * POST   /api/hogares/{h}/genericos                  crea uno propio (201)
 * DELETE /api/hogares/{h}/genericos/{id}             borra uno propio
 * PUT    /api/hogares/{h}/genericos/clasificacion    corrige el genérico de un producto de ticket
 * PUT    /api/hogares/{h}/genericos/clasificacion-web  corrige el de un producto de la web (común)
 * GET    /api/hogares/{h}/genericos/ticket/{ticket}  el genérico de cada línea de un ticket
 */
@RestController
@RequestMapping("/api/hogares/{hogarId}/genericos")
@RequiredArgsConstructor
public class GenericoController {

    private final GenericoService servicio;

    @GetMapping
    public ApiResponse<List<GenericoResumenDto>> catalogo(@CurrentUser AuthUser yo, @PathVariable UUID hogarId) {
        return ApiResponse.ok(servicio.catalogo(hogarId, yo.id()));
    }

    @GetMapping("/{genericoId}")
    public ApiResponse<GenericoDetalleDto> detalle(@CurrentUser AuthUser yo, @PathVariable UUID hogarId,
                                                   @PathVariable UUID genericoId) {
        return ApiResponse.ok(servicio.detalle(hogarId, genericoId, yo.id()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GenericoDto> crear(@CurrentUser AuthUser yo, @PathVariable UUID hogarId,
                                          @Valid @RequestBody NuevoGenericoRequest peticion) {
        return ApiResponse.ok(servicio.crear(hogarId, yo.id(), peticion));
    }

    @DeleteMapping("/{genericoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrar(@CurrentUser AuthUser yo, @PathVariable UUID hogarId, @PathVariable UUID genericoId) {
        servicio.borrar(hogarId, genericoId, yo.id());
    }

    @PutMapping("/clasificacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clasificar(@CurrentUser AuthUser yo, @PathVariable UUID hogarId,
                           @Valid @RequestBody ClasificarRequest peticion) {
        servicio.clasificar(hogarId, yo.id(), peticion);
    }

    @PutMapping("/clasificacion-web")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clasificarWeb(@CurrentUser AuthUser yo, @PathVariable UUID hogarId,
                              @Valid @RequestBody ClasificarWebRequest peticion) {
        servicio.clasificarWeb(hogarId, yo.id(), peticion);
    }

    @GetMapping("/ticket/{ticketId}")
    public ApiResponse<List<ClasificacionLineaDto>> deTicket(@CurrentUser AuthUser yo, @PathVariable UUID hogarId,
                                                             @PathVariable UUID ticketId) {
        return ApiResponse.ok(servicio.deTicket(hogarId, ticketId, yo.id()));
    }
}
