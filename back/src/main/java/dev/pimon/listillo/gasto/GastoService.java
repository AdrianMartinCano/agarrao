package dev.pimon.listillo.gasto;

import dev.pimon.common.exception.AppException;
import dev.pimon.listillo.gasto.dto.CategoriaGastoDto;
import dev.pimon.listillo.gasto.dto.GastoDto;
import dev.pimon.listillo.gasto.dto.GenericoGastoDto;
import dev.pimon.listillo.gasto.dto.MesDto;
import dev.pimon.listillo.gasto.dto.TiendaGastoDto;
import dev.pimon.listillo.gasto.dto.TipoGastoDto;
import dev.pimon.listillo.gasolina.repostajes.Repostaje;
import dev.pimon.listillo.generico.Categoria;
import dev.pimon.listillo.generico.Clasificacion;
import dev.pimon.listillo.generico.ClasificacionRepository;
import dev.pimon.listillo.generico.Compra;
import dev.pimon.listillo.generico.Generico;
import dev.pimon.listillo.generico.GenericoRepository;
import dev.pimon.listillo.hogar.AccesoHogar;
import dev.pimon.listillo.ticket.Cadena;
import dev.pimon.listillo.ticket.Ticket;
import dev.pimon.listillo.ticket.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * El gasto de un hogar por mes: sus tickets del súper (por tienda, categoría y genérico) y
 * lo anotado aparte (restaurantes, suscripciones, gasolina…), todo junto por tipo.
 *
 * El total y las tiendas cuentan el TOTAL de cada ticket (lo que se pagó). Las categorías
 * y los genéricos cuentan sus líneas, sin los descuentos, que van aparte: un «-1,20 €» no
 * es de ninguna categoría.
 */
@Service
@RequiredArgsConstructor
public class GastoService {

    static final int MESES = 6;
    static final int TOP = 5;
    static final int MESES_DE_REFERENCIA = 12;

    private final AccesoHogar acceso;
    private final TicketRepository tickets;
    private final ClasificacionRepository clasificaciones;
    private final GenericoRepository genericos;
    private final GastoRepository anotados;
    private final dev.pimon.listillo.gasolina.repostajes.RepostajeRepository repostajes;

    @Transactional(readOnly = true)
    public GastoDto resumen(UUID hogarId, String usuarioId, String mesTexto) {
        acceso.exigirMiembro(hogarId, usuarioId);
        YearMonth mes = mes(mesTexto);
        // Los tarritos acaban en el mes de hoy mientras el elegido quepa en ellos: elegir
        // septiembre no puede esconder octubre. Uno más viejo se ve al final de los suyos.
        YearMonth hoy = YearMonth.now();
        YearMonth ultimo = !mes.isAfter(hoy) && mes.isAfter(hoy.minusMonths(MESES)) ? hoy : mes;
        LocalDate desde = ultimo.minusMonths(MESES - 1L).atDay(1);
        LocalDate hasta = ultimo.atEndOfMonth();

        List<Ticket> delPeriodo = tickets.findByHogarIdAndFechaBetween(hogarId, desde, hasta);
        Map<YearMonth, List<Ticket>> porMes = delPeriodo.stream().collect(Collectors.groupingBy(t -> YearMonth.from(t.getFecha())));
        Map<YearMonth, List<Gasto>> anotadosPorMes = anotados
                .findByHogarIdAndFechaBetweenOrderByFechaDescCreadoEnDesc(hogarId, desde, hasta).stream()
                .collect(Collectors.groupingBy(g -> YearMonth.from(g.getFecha())));
        // Los repostajes del cuaderno de gasolina cuentan como GASOLINA (no se copian a gastos).
        Map<YearMonth, List<Repostaje>> repostajesPorMes = repostajes
                .findByHogarIdAndFechaBetween(hogarId, desde, hasta).stream()
                .collect(Collectors.groupingBy(r -> YearMonth.from(r.getFecha())));
        List<MesDto> meses = new ArrayList<>();
        for (YearMonth m = ultimo.minusMonths(MESES - 1L); !m.isAfter(ultimo); m = m.plusMonths(1)) {
            List<Ticket> suyos = porMes.getOrDefault(m, List.of());
            meses.add(new MesDto(m, suma(suyos)
                    .add(sumaAnotados(anotadosPorMes.getOrDefault(m, List.of())))
                    .add(sumaRepostajes(repostajesPorMes.getOrDefault(m, List.of()))), suyos.size()));
        }

        List<Ticket> delMes = porMes.getOrDefault(mes, List.of());
        List<Gasto> anotadosDelMes = anotadosPorMes.getOrDefault(mes, List.of());
        List<Repostaje> repostajesDelMes = repostajesPorMes.getOrDefault(mes, List.of());
        Map<Cadena, List<Ticket>> porCadena = delMes.stream()
                .collect(Collectors.groupingBy(Ticket::getCadena, () -> new EnumMap<>(Cadena.class), Collectors.toList()));
        List<TiendaGastoDto> porTienda = porCadena.entrySet().stream()
                .map(e -> new TiendaGastoDto(e.getKey(), suma(e.getValue()), e.getValue().size()))
                .sorted(Comparator.comparing(TiendaGastoDto::total).reversed())
                .toList();

        Lineas lineas = lineasDelMes(hogarId, mes);
        BigDecimal total = suma(delMes).add(sumaAnotados(anotadosDelMes)).add(sumaRepostajes(repostajesDelMes));
        return new GastoDto(mes, total, delMes.size(), anotadosDelMes.size(), repostajesDelMes.size(),
                lineas.descuentos(), referencia(hogarId), meses, porTipo(delMes, anotadosDelMes, repostajesDelMes),
                porTienda, lineas.porCategoria(), lineas.top());
    }

    /**
     * Los tickets del súper son SUPER; los repostajes, GASOLINA; lo anotado, del tipo que se
     * le dio. De mayor a menor.
     */
    private static List<TipoGastoDto> porTipo(List<Ticket> tickets, List<Gasto> anotados, List<Repostaje> repostajes) {
        Map<TipoGasto, BigDecimal> total = new EnumMap<>(TipoGasto.class);
        Map<TipoGasto, Integer> veces = new EnumMap<>(TipoGasto.class);
        if (!tickets.isEmpty()) {
            total.put(TipoGasto.SUPER, suma(tickets));
            veces.put(TipoGasto.SUPER, tickets.size());
        }
        for (Gasto g : anotados) {
            total.merge(g.getTipo(), g.getImporte(), BigDecimal::add);
            veces.merge(g.getTipo(), 1, Integer::sum);
        }
        if (!repostajes.isEmpty()) {
            total.merge(TipoGasto.GASOLINA, sumaRepostajes(repostajes), BigDecimal::add);
            veces.merge(TipoGasto.GASOLINA, repostajes.size(), Integer::sum);
        }
        return total.entrySet().stream()
                .map(e -> new TipoGastoDto(e.getKey(), e.getValue(), veces.get(e.getKey())))
                .sorted(Comparator.comparing(TipoGastoDto::total).reversed())
                .toList();
    }

    private static BigDecimal sumaRepostajes(List<Repostaje> lista) {
        return lista.stream().map(Repostaje::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumaAnotados(List<Gasto> gastos) {
        return gastos.stream().map(Gasto::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Lineas lineasDelMes(UUID hogarId, YearMonth mes) {
        Map<Clasificacion.Clave, UUID> genericoDe = new HashMap<>();
        for (Clasificacion c : clasificaciones.findByClaveHogarId(hogarId)) {
            if (c.getGenericoId() != null) {
                genericoDe.put(c.getClave(), c.getGenericoId());
            }
        }
        Map<UUID, Generico> porId = genericos.delHogar(hogarId).stream()
                .collect(Collectors.toMap(Generico::getId, Function.identity()));

        BigDecimal descuentos = BigDecimal.ZERO;
        // HashMap y no EnumMap: la clave null son las líneas sin genérico.
        Map<Categoria, BigDecimal> porCategoria = new HashMap<>();
        Map<UUID, BigDecimal> porGenerico = new LinkedHashMap<>();
        Map<UUID, Integer> compras = new HashMap<>();
        for (Compra c : clasificaciones.compras(hogarId, mes.atDay(1))) {
            if (c.fecha().isAfter(mes.atEndOfMonth())) {
                continue;
            }
            if (c.importe().signum() < 0) {
                descuentos = descuentos.add(c.importe().negate());
                continue;
            }
            Generico g = porId.get(genericoDe.get(c.clave(hogarId)));
            porCategoria.merge(g == null ? null : g.getCategoria(), c.importe(), BigDecimal::add);
            if (g != null) {
                porGenerico.merge(g.getId(), c.importe(), BigDecimal::add);
                compras.merge(g.getId(), 1, Integer::sum);
            }
        }
        List<CategoriaGastoDto> categorias = porCategoria.entrySet().stream()
                .map(e -> new CategoriaGastoDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(CategoriaGastoDto::total).reversed())
                .toList();
        List<GenericoGastoDto> top = porGenerico.entrySet().stream()
                .sorted(Map.Entry.<UUID, BigDecimal>comparingByValue().reversed())
                .limit(TOP)
                .map(e -> {
                    Generico g = porId.get(e.getKey());
                    return new GenericoGastoDto(g.getId(), g.getNombre(), g.getCategoria(), g.getRetrato(), e.getValue(),
                            compras.get(g.getId()));
                })
                .toList();
        return new Lineas(descuentos, categorias, top);
    }

    /** El mes más caro de los últimos doce hasta hoy (incluido el actual), tickets y anotados juntos. */
    private BigDecimal referencia(UUID hogarId) {
        YearMonth hoy = YearMonth.now();
        LocalDate desde = hoy.minusMonths(MESES_DE_REFERENCIA - 1L).atDay(1);
        Map<YearMonth, BigDecimal> porMes = new HashMap<>();
        for (Ticket t : tickets.findByHogarIdAndFechaBetween(hogarId, desde, hoy.atEndOfMonth())) {
            porMes.merge(YearMonth.from(t.getFecha()), t.getTotal(), BigDecimal::add);
        }
        for (Gasto g : anotados.findByHogarIdAndFechaBetweenOrderByFechaDescCreadoEnDesc(hogarId, desde, hoy.atEndOfMonth())) {
            porMes.merge(YearMonth.from(g.getFecha()), g.getImporte(), BigDecimal::add);
        }
        for (Repostaje r : repostajes.findByHogarIdAndFechaBetween(hogarId, desde, hoy.atEndOfMonth())) {
            porMes.merge(YearMonth.from(r.getFecha()), r.getImporte(), BigDecimal::add);
        }
        return porMes.values().stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
    }

    /** «2026-10»; sin mes, el actual. */
    static YearMonth mes(String texto) {
        if (texto == null || texto.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(texto.strip());
        } catch (DateTimeParseException e) {
            throw AppException.badRequest("El mes va como 2026-10");
        }
    }

    private static BigDecimal suma(List<Ticket> tickets) {
        return tickets.stream().map(Ticket::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record Lineas(BigDecimal descuentos, List<CategoriaGastoDto> porCategoria, List<GenericoGastoDto> top) {
    }
}
