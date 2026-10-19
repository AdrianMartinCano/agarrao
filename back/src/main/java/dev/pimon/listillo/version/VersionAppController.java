package dev.pimon.listillo.version;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.listillo.version.dto.ActualizarVersionRequest;
import dev.pimon.listillo.version.dto.ComprobacionVersionDto;
import dev.pimon.listillo.version.dto.VersionAppDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/app/version?actual=1.0.0   (público: se pregunta antes de entrar)
 * GET /api/admin/app/version          (ROLE_ADMIN)
 * PUT /api/admin/app/version          (ROLE_ADMIN)
 */
@RestController
@RequiredArgsConstructor
public class VersionAppController {

    private final VersionAppService servicio;

    @GetMapping("/api/app/version")
    public ApiResponse<ComprobacionVersionDto> comprobar(@RequestParam String actual) {
        return ApiResponse.ok(servicio.comprobar(actual));
    }

    @GetMapping("/api/admin/app/version")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<VersionAppDto> obtener() {
        return ApiResponse.ok(servicio.obtener());
    }

    @PutMapping("/api/admin/app/version")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<VersionAppDto> actualizar(@Valid @RequestBody ActualizarVersionRequest peticion) {
        return ApiResponse.ok(servicio.actualizar(peticion));
    }
}
