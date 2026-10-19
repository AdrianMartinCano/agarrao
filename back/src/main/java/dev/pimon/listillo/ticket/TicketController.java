package dev.pimon.listillo.ticket;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.common.exception.AppException;
import dev.pimon.listillo.ticket.dto.BorradorDto;
import dev.pimon.listillo.ticket.dto.GuardadoDto;
import dev.pimon.listillo.ticket.dto.GuardarTicketRequest;
import dev.pimon.listillo.ticket.dto.LeerTextoRequest;
import dev.pimon.listillo.ticket.dto.TicketDto;
import dev.pimon.listillo.ticket.dto.TicketResumenDto;
import dev.pimon.security.annotation.CurrentUser;
import dev.pimon.security.model.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * POST   /api/hogares/{h}/tickets/leer-pdf    multipart «archivo»: lee un ticket digital (no guarda)
 * POST   /api/hogares/{h}/tickets/leer-texto  {texto}: lee el texto de una foto (no guarda)
 * POST   /api/hogares/{h}/tickets             guarda el ticket repasado (201)
 * GET    /api/hogares/{h}/tickets             los últimos 100
 * GET    /api/hogares/{h}/tickets/{id}        con sus líneas
 * DELETE /api/hogares/{h}/tickets/{id}
 */
@RestController
@RequestMapping("/api/hogares/{hogarId}/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService servicio;

    @PostMapping(path = "/leer-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<BorradorDto> leerPdf(@CurrentUser AuthUser yo, @PathVariable UUID hogarId,
                                            @RequestParam("archivo") MultipartFile archivo) {
        try {
            return ApiResponse.ok(servicio.leerPdf(hogarId, yo.id(), archivo.getBytes()));
        } catch (IOException e) {
            throw AppException.badRequest("No ha llegado bien el archivo");
        }
    }

    @PostMapping("/leer-texto")
    public ApiResponse<BorradorDto> leerTexto(@CurrentUser AuthUser yo, @PathVariable UUID hogarId,
                                              @Valid @RequestBody LeerTextoRequest peticion) {
        return ApiResponse.ok(servicio.leerTexto(hogarId, yo.id(), peticion.texto()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GuardadoDto> guardar(@CurrentUser AuthUser yo, @PathVariable UUID hogarId,
                                            @Valid @RequestBody GuardarTicketRequest peticion) {
        return ApiResponse.ok(servicio.guardar(hogarId, yo.id(), peticion));
    }

    @GetMapping
    public ApiResponse<List<TicketResumenDto>> listar(@CurrentUser AuthUser yo, @PathVariable UUID hogarId) {
        return ApiResponse.ok(servicio.listar(hogarId, yo.id()));
    }

    @GetMapping("/{ticketId}")
    public ApiResponse<TicketDto> detalle(@CurrentUser AuthUser yo, @PathVariable UUID hogarId,
                                          @PathVariable UUID ticketId) {
        return ApiResponse.ok(servicio.detalle(hogarId, ticketId, yo.id()));
    }

    @DeleteMapping("/{ticketId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrar(@CurrentUser AuthUser yo, @PathVariable UUID hogarId, @PathVariable UUID ticketId) {
        servicio.borrar(hogarId, ticketId, yo.id());
    }
}
