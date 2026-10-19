package dev.pimon.listillo.version;

import dev.pimon.common.exception.AppException;
import dev.pimon.listillo.version.dto.ActualizarVersionRequest;
import dev.pimon.listillo.version.dto.ComprobacionVersionDto;
import dev.pimon.listillo.version.dto.VersionAppDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VersionAppService {

    private final VersionAppRepository repositorio;

    /**
     * Compara la versión instalada con la publicada.
     *
     * Una versión instalada MAYOR que la publicada es normal —es quien prueba la siguiente
     * antes de anunciarla— y cuenta como al día.
     */
    @Transactional(readOnly = true)
    public ComprobacionVersionDto comprobar(String actual) {
        if (!Semver.esValida(actual)) {
            throw AppException.badRequest("La versión instalada debe ser x.y.z: " + actual);
        }
        VersionApp publicada = buscar();

        EstadoVersion estado;
        if (Semver.comparar(actual, publicada.getMinima()) < 0) {
            estado = EstadoVersion.OBLIGATORIA;
        } else if (Semver.comparar(actual, publicada.getUltima()) < 0) {
            estado = EstadoVersion.OPCIONAL;
        } else {
            estado = EstadoVersion.AL_DIA;
        }

        return new ComprobacionVersionDto(estado, actual, publicada.getUltima(),
                publicada.getMinima(), publicada.getNotas());
    }

    @Transactional(readOnly = true)
    public VersionAppDto obtener() {
        return aDto(buscar());
    }

    /**
     * Cambia la versión publicada. La mínima no puede pasar de la última: exigir una
     * versión que todavía no existe dejaría a todo el mundo bloqueado sin forma de salir.
     */
    @Transactional
    public VersionAppDto actualizar(ActualizarVersionRequest peticion) {
        if (Semver.comparar(peticion.minima(), peticion.ultima()) > 0) {
            throw AppException.badRequest("La mínima (" + peticion.minima()
                    + ") no puede ser mayor que la última (" + peticion.ultima() + ")");
        }
        VersionApp version = buscar();
        version.actualizar(peticion.ultima(), peticion.minima(), peticion.notas());
        return aDto(repositorio.saveAndFlush(version));
    }

    /** La fila la crea la migración V2; si falta, la base está mal y es un 500 legítimo. */
    private VersionApp buscar() {
        return repositorio.findById(VersionApp.ID)
                .orElseThrow(() -> AppException.internalError("Falta la fila de version_app (migración V2)"));
    }

    private static VersionAppDto aDto(VersionApp v) {
        return new VersionAppDto(v.getUltima(), v.getMinima(), v.getNotas(), v.getActualizadaEn());
    }
}
