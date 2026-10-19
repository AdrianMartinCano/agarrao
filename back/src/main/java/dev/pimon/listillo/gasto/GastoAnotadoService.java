package dev.pimon.listillo.gasto;

import dev.pimon.common.exception.AppException;
import dev.pimon.listillo.gasto.dto.GastoAnotadoDto;
import dev.pimon.listillo.gasto.dto.GastoFijoDto;
import dev.pimon.listillo.gasto.dto.GastoRequest;
import dev.pimon.listillo.hogar.AccesoHogar;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Los gastos que no son tickets del súper: anotarlos, cambiarlos, borrarlos, y los fijos de
 * cada mes. No se reparten entre los de casa: solo se apuntan para verlos en Gasto.
 */
@Service
@RequiredArgsConstructor
public class GastoAnotadoService {

    private final AccesoHogar acceso;
    private final GastoRepository gastos;
    private final GastoFijoRepository fijos;

    @Transactional(readOnly = true)
    public List<GastoAnotadoDto> delMes(UUID hogarId, String usuarioId, YearMonth mes) {
        acceso.exigirMiembro(hogarId, usuarioId);
        return gastos.findByHogarIdAndFechaBetweenOrderByFechaDescCreadoEnDesc(hogarId, mes.atDay(1), mes.atEndOfMonth())
                .stream().map(GastoAnotadoService::aDto).toList();
    }

    @Transactional
    public GastoAnotadoDto anotar(UUID hogarId, String usuarioId, GastoRequest peticion) {
        acceso.exigirMiembro(hogarId, usuarioId);
        String concepto = peticion.concepto().strip();
        UUID fijoId = null;
        if (peticion.cadaMes()) {
            GastoFijo fijo = fijos.save(new GastoFijo(hogarId, concepto, peticion.importe(), peticion.tipo(),
                    peticion.fecha(), usuarioId));
            fijoId = fijo.getId();
        }
        Gasto gasto = gastos.save(new Gasto(hogarId, concepto, peticion.importe(), peticion.fecha(), peticion.tipo(),
                fijoId, usuarioId));
        if (fijoId != null) {
            // Si se anota con fecha de hace meses, se apuntan también los que ya han pasado.
            gastos.saveAll(fijos.getReferenceById(fijoId).pendientes(LocalDate.now()));
        }
        return aDto(gasto);
    }

    @Transactional(readOnly = true)
    public GastoAnotadoDto uno(UUID hogarId, UUID gastoId, String usuarioId) {
        acceso.exigirMiembro(hogarId, usuarioId);
        return aDto(buscar(hogarId, gastoId));
    }

    @Transactional
    public GastoAnotadoDto cambiar(UUID hogarId, UUID gastoId, String usuarioId, GastoRequest peticion) {
        acceso.exigirMiembro(hogarId, usuarioId);
        Gasto gasto = buscar(hogarId, gastoId);
        gasto.cambiar(peticion.concepto().strip(), peticion.importe(), peticion.fecha(), peticion.tipo());
        return aDto(gasto);
    }

    @Transactional
    public void borrar(UUID hogarId, UUID gastoId, String usuarioId) {
        acceso.exigirMiembro(hogarId, usuarioId);
        gastos.delete(buscar(hogarId, gastoId));
    }

    @Transactional(readOnly = true)
    public List<GastoFijoDto> fijos(UUID hogarId, String usuarioId) {
        acceso.exigirMiembro(hogarId, usuarioId);
        return fijos.findByHogarIdOrderByConceptoAsc(hogarId).stream()
                .map(f -> new GastoFijoDto(f.getId(), f.getConcepto(), f.getImporte(), f.getTipo(), f.getDia()))
                .toList();
    }

    /** Deja de apuntarse cada mes. Los meses ya apuntados se quedan (su fijo_id pasa a null). */
    @Transactional
    public void quitarFijo(UUID hogarId, UUID fijoId, String usuarioId) {
        acceso.exigirMiembro(hogarId, usuarioId);
        fijos.delete(fijos.findByIdAndHogarId(fijoId, hogarId)
                .orElseThrow(() -> AppException.notFound("Ese gasto fijo no existe")));
    }

    /** Apunta los meses de los gastos fijos que ya han llegado a su día. */
    @Transactional
    public void ponerAlDia(UUID hogarId, String usuarioId) {
        acceso.exigirMiembro(hogarId, usuarioId);
        LocalDate hoy = LocalDate.now();
        for (GastoFijo fijo : fijos.findByHogarIdOrderByConceptoAsc(hogarId)) {
            gastos.saveAll(fijo.pendientes(hoy));
        }
    }

    private Gasto buscar(UUID hogarId, UUID gastoId) {
        return gastos.findByIdAndHogarId(gastoId, hogarId)
                .orElseThrow(() -> AppException.notFound("Ese gasto no existe"));
    }

    private static GastoAnotadoDto aDto(Gasto g) {
        return new GastoAnotadoDto(g.getId(), g.getConcepto(), g.getImporte(), g.getFecha(), g.getTipo(), g.getFijoId());
    }
}
