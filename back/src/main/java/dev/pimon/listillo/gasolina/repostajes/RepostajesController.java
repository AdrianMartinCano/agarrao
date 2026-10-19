package dev.pimon.listillo.gasolina.repostajes;

import dev.pimon.listillo.hogar.AccesoHogar;
import java.util.UUID;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.listillo.gasolina.repostajes.dto.RepostajesDtos.*;
import dev.pimon.security.annotation.CurrentUser;
import dev.pimon.security.model.AuthUser;
import jakarta.validation.Valid;
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

/**
 * Vehículos y repostajes del HOGAR: un coche de casa lo ven todos los de
 * casa y cada repostaje cuenta una sola vez en el gasto. Viene de Gasofa, donde eran de
 * cada usuario.
 *
 * GET/POST       /api/hogares/{h}/vehiculos · PUT/DELETE /vehiculos/{id} (DELETE archiva)
 * GET            /api/hogares/{h}/vehiculos/{id}/estadisticas
 * GET/POST       /api/hogares/{h}/repostajes · PUT/DELETE /repostajes/{id}
 */
@RestController
@RequestMapping("/api/hogares/{hogarId}")
public class RepostajesController {

    private final RepostajesService servicio;
    private final AccesoHogar acceso;

    public RepostajesController(RepostajesService servicio, AccesoHogar acceso) {
        this.servicio = servicio;
        this.acceso = acceso;
    }

    /** Todo cuelga del hogar: quien no es de él recibe 404 (como el resto de la app). */
    private UUID hogar(UUID hogarId, AuthUser usuario) {
        acceso.exigirMiembro(hogarId, usuario.id());
        return hogarId;
    }

    // ── Vehículos ── //

    @GetMapping("/vehiculos")
    public ApiResponse<List<VehiculoDto>> vehiculos(@CurrentUser AuthUser usuario, @PathVariable UUID hogarId) {
        return ApiResponse.ok(servicio.listarVehiculos(hogar(hogarId, usuario)));
    }

    @PostMapping("/vehiculos")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<VehiculoDto> crearVehiculo(@CurrentUser AuthUser usuario, @PathVariable UUID hogarId,
                                                  @Valid @RequestBody CrearVehiculoRequest peticion) {
        return ApiResponse.ok(servicio.crearVehiculo(hogar(hogarId, usuario), peticion), "Vehículo añadido");
    }

    /**
     * Corrige nombre, carburante o matrícula.
     *
     * Cambiar el carburante no reescribe los repostajes ya apuntados: cada uno guarda el
     * suyo. Solo cambia lo que se propone la próxima vez.
     */
    @PutMapping("/vehiculos/{id}")
    public ApiResponse<VehiculoDto> actualizarVehiculo(
            @CurrentUser AuthUser usuario, @PathVariable UUID hogarId,
            @PathVariable String id,
            @Valid @RequestBody ActualizarVehiculoRequest peticion) {

        return ApiResponse.ok(servicio.actualizarVehiculo(hogar(hogarId, usuario), id, peticion),
                "Vehículo actualizado");
    }

    /** Lo archiva: el historial de repostajes se conserva. */
    @DeleteMapping("/vehiculos/{id}")
    public ApiResponse<Void> archivarVehiculo(@CurrentUser AuthUser usuario, @PathVariable UUID hogarId, @PathVariable String id) {
        servicio.archivarVehiculo(hogar(hogarId, usuario), id);
        return ApiResponse.noContent("Vehículo archivado. Sus repostajes se conservan.");
    }

    @GetMapping("/vehiculos/{id}/estadisticas")
    public ApiResponse<EstadisticasDto> estadisticas(@CurrentUser AuthUser usuario, @PathVariable UUID hogarId,
                                                     @PathVariable String id) {
        return ApiResponse.ok(servicio.estadisticas(hogar(hogarId, usuario), id));
    }

    // ── Repostajes ── //

    @GetMapping("/repostajes")
    public ApiResponse<List<RepostajeDto>> repostajes(@CurrentUser AuthUser usuario, @PathVariable UUID hogarId) {
        return ApiResponse.ok(servicio.listar(hogar(hogarId, usuario)));
    }

    @PostMapping("/repostajes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RepostajeDto> crear(@CurrentUser AuthUser usuario, @PathVariable UUID hogarId,
                                           @Valid @RequestBody CrearRepostajeRequest peticion) {
        return ApiResponse.ok(servicio.crear(hogar(hogarId, usuario), usuario.id(), peticion), "Repostaje guardado");
    }

    /**
     * Corrige un repostaje ya apuntado.
     *
     * El precio medio de la provincia solo se recalcula si cambia la gasolinera: es un
     * dato con fecha, y meterle el de hoy a un repostaje de hace un mes falsearía la
     * comparación.
     */
    @PutMapping("/repostajes/{id}")
    public ApiResponse<RepostajeDto> actualizar(@CurrentUser AuthUser usuario, @PathVariable UUID hogarId,
                                                @PathVariable String id,
                                                @Valid @RequestBody ActualizarRepostajeRequest peticion) {
        return ApiResponse.ok(servicio.actualizar(hogar(hogarId, usuario), id, peticion), "Repostaje actualizado");
    }

    @DeleteMapping("/repostajes/{id}")
    public ApiResponse<Void> eliminar(@CurrentUser AuthUser usuario, @PathVariable UUID hogarId, @PathVariable String id) {
        servicio.eliminar(hogar(hogarId, usuario), id);
        return ApiResponse.noContent("Repostaje eliminado");
    }
}
