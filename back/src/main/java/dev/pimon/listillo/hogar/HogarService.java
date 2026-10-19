package dev.pimon.listillo.hogar;

import dev.pimon.common.exception.AppException;
import dev.pimon.listillo.hogar.dto.HogarDto;
import dev.pimon.listillo.hogar.dto.HogarResumenDto;
import dev.pimon.listillo.hogar.dto.InvitacionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HogarService {

    /** Una familia grande, compañeros de piso… Más ya no es un hogar. */
    static final int MAX_MIEMBROS = 12;
    /** La tuya, la de tus padres, la del pueblo… */
    static final int MAX_HOGARES = 5;
    private static final int INTENTOS_CODIGO = 5;

    private final HogarRepository hogares;
    private final MiembroHogarRepository miembros;
    private final AccesoHogar acceso;

    @Transactional(readOnly = true)
    public List<HogarResumenDto> misHogares(String usuarioId) {
        return miembros.resumenesDe(usuarioId);
    }

    @Transactional(readOnly = true)
    public HogarDto detalle(UUID hogarId, String usuarioId) {
        MiembroHogar yo = acceso.exigirMiembro(hogarId, usuarioId);
        return aDto(buscar(hogarId), yo);
    }

    @Transactional
    public HogarDto crear(String usuarioId, String nombre) {
        exigirHuecoParaOtroHogar(usuarioId);
        Hogar hogar = hogares.save(new Hogar(nombre.strip(), codigoLibre()));
        MiembroHogar yo = miembros.save(new MiembroHogar(hogar.getId(), usuarioId, RolMiembro.PROPIETARIO));
        miembros.flush();
        return aDto(hogar, yo);
    }

    /**
     * A qué hogar lleva un código, para confirmarlo antes de entrar. Pide sesión como todo
     * lo demás: el nombre de un hogar no se enseña a cualquiera que pruebe códigos.
     */
    @Transactional(readOnly = true)
    public InvitacionDto invitacion(String codigoEscrito, String usuarioId) {
        Hogar hogar = buscarPorCodigo(codigoEscrito);
        return new InvitacionDto(hogar.getNombre(), miembros.countByIdHogarId(hogar.getId()),
                miembros.existsById(new MiembroHogarId(hogar.getId(), usuarioId)));
    }

    /** Entrar dos veces con el mismo código no es un error: devuelve el hogar sin más. */
    @Transactional
    public HogarDto unirse(String usuarioId, String codigoEscrito) {
        Hogar hogar = buscarPorCodigo(codigoEscrito);

        Optional<MiembroHogar> yaDentro = miembros.findById(new MiembroHogarId(hogar.getId(), usuarioId));
        if (yaDentro.isPresent()) {
            return aDto(hogar, yaDentro.get());
        }
        exigirHuecoParaOtroHogar(usuarioId);
        if (miembros.countByIdHogarId(hogar.getId()) >= MAX_MIEMBROS) {
            throw AppException.conflict("Ese hogar ya está lleno (" + MAX_MIEMBROS + " personas)");
        }
        MiembroHogar yo = miembros.save(new MiembroHogar(hogar.getId(), usuarioId, RolMiembro.MIEMBRO));
        miembros.flush();
        return aDto(hogar, yo);
    }

    /** Cualquiera de la casa puede cambiarle el nombre: no es una decisión importante. */
    @Transactional
    public HogarDto renombrar(UUID hogarId, String usuarioId, String nombre) {
        MiembroHogar yo = acceso.exigirMiembro(hogarId, usuarioId);
        Hogar hogar = buscar(hogarId);
        hogar.renombrar(nombre.strip());
        return aDto(hogar, yo);
    }

    /** Qué supermercados ve y compara la casa. Cualquiera de la casa puede cambiarlo. */
    @Transactional
    public HogarDto elegirCadenas(UUID hogarId, String usuarioId, java.util.List<dev.pimon.listillo.ticket.Cadena> cadenas) {
        MiembroHogar yo = acceso.exigirMiembro(hogarId, usuarioId);
        Hogar hogar = buscar(hogarId);
        java.util.Set<dev.pimon.listillo.ticket.Cadena> elegidas =
                java.util.EnumSet.noneOf(dev.pimon.listillo.ticket.Cadena.class);
        elegidas.addAll(cadenas);
        elegidas.remove(dev.pimon.listillo.ticket.Cadena.OTRA);
        if (elegidas.isEmpty()) {
            throw AppException.badRequest("Elige al menos un supermercado");
        }
        hogar.elegirCadenas(elegidas);
        return aDto(hogar, yo);
    }

    @Transactional
    public HogarDto nuevoCodigo(UUID hogarId, String usuarioId) {
        MiembroHogar yo = exigirPropietario(hogarId, usuarioId);
        Hogar hogar = buscar(hogarId);
        hogar.cambiarCodigo(codigoLibre());
        return aDto(hogar, yo);
    }

    /**
     * Irse del hogar. El último en irse se lo lleva con su lista; si se va el propietario y
     * queda gente, el puesto pasa al miembro más antiguo.
     */
    @Transactional
    public void salir(UUID hogarId, String usuarioId) {
        MiembroHogar yo = acceso.exigirMiembro(hogarId, usuarioId);
        miembros.delete(yo);
        miembros.flush();

        List<MiembroHogar> quedan = miembros.findByIdHogarIdOrderByUnidoEn(hogarId);
        if (quedan.isEmpty()) {
            hogares.deleteById(hogarId);
        } else if (yo.esPropietario()) {
            quedan.getFirst().hacerPropietario();
        }
    }

    @Transactional
    public void echar(UUID hogarId, String usuarioId, String otroId) {
        exigirPropietario(hogarId, usuarioId);
        if (usuarioId.equals(otroId)) {
            throw AppException.badRequest("Para irte tú, usa «Salir del hogar»");
        }
        MiembroHogar otro = miembros.findById(new MiembroHogarId(hogarId, otroId))
                .orElseThrow(() -> AppException.notFound("Esa persona ya no está en el hogar"));
        miembros.delete(otro);
    }

    private MiembroHogar exigirPropietario(UUID hogarId, String usuarioId) {
        MiembroHogar yo = acceso.exigirMiembro(hogarId, usuarioId);
        if (!yo.esPropietario()) {
            throw AppException.forbidden("Solo quien lleva el hogar puede hacer esto");
        }
        return yo;
    }

    private void exigirHuecoParaOtroHogar(String usuarioId) {
        if (miembros.countByIdUsuarioId(usuarioId) >= MAX_HOGARES) {
            throw AppException.conflict(
                    "Ya estás en " + MAX_HOGARES + " hogares: sal de alguno para entrar en otro");
        }
    }

    /** Choque de códigos: casi imposible, pero un UNIQUE roto sería un 500 feo. */
    private String codigoLibre() {
        for (int i = 0; i < INTENTOS_CODIGO; i++) {
            String codigo = CodigoInvitacion.nuevo();
            if (!hogares.existsByCodigoInvitacion(codigo)) {
                return codigo;
            }
        }
        throw AppException.internalError("No se pudo generar un código de invitación libre");
    }

    private Hogar buscarPorCodigo(String codigoEscrito) {
        return hogares.findByCodigoInvitacion(CodigoInvitacion.normalizar(codigoEscrito))
                .orElseThrow(() -> AppException.notFound(
                        "Ese código no es de ningún hogar. Revísalo o pide uno nuevo"));
    }

    private Hogar buscar(UUID hogarId) {
        return hogares.findById(hogarId)
                .orElseThrow(() -> AppException.notFound("Ese hogar no existe o no eres de él"));
    }

    private HogarDto aDto(Hogar hogar, MiembroHogar yo) {
        return new HogarDto(hogar.getId(), hogar.getNombre(), hogar.getCodigoInvitacion(),
                yo.getRol(), miembros.miembrosDe(hogar.getId()),
                hogar.cadenasVisibles().stream().filter(c -> c != dev.pimon.listillo.ticket.Cadena.OTRA).toList());
    }
}
