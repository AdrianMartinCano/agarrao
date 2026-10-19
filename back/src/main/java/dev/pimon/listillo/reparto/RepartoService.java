package dev.pimon.listillo.reparto;

import dev.pimon.listillo.generico.Adivinador;
import dev.pimon.listillo.generico.Clasificacion;
import dev.pimon.listillo.generico.ClasificacionRepository;
import dev.pimon.listillo.generico.Compra;
import dev.pimon.listillo.generico.Generico;
import dev.pimon.listillo.generico.GenericoRepository;
import dev.pimon.listillo.generico.GenericoService;
import dev.pimon.listillo.generico.dto.PrecioTiendaDto;
import dev.pimon.listillo.hogar.AccesoHogar;
import dev.pimon.listillo.lista.ArticuloLista;
import dev.pimon.listillo.lista.ArticuloListaRepository;
import dev.pimon.listillo.producto.Producto;
import dev.pimon.listillo.producto.ProductoRepository;
import dev.pimon.listillo.reparto.dto.RepartoDto;
import dev.pimon.listillo.ticket.Cadena;
import dev.pimon.listillo.ticket.Textos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reparte lo que falta de la lista entre los súper: cada cosa va a la cadena donde su
 * genérico sale más barato por unidad (web de hoy o último ticket).
 *
 * Qué genérico es cada cosa de la lista, por este orden:
 * <ol>
 *   <li>lo que dicen los tickets: una línea casada con ese mismo nombre y ya clasificada;</li>
 *   <li>el producto escaneado (categoría de Open Food Facts y su nombre);</li>
 *   <li>el nombre apuntado, con el mismo {@link Adivinador} que los tickets.</li>
 * </ol>
 *
 * Para que los totales se puedan comparar entre tiendas, cada cosa se cuenta en TODAS con la
 * misma cantidad: la del envase más barato por unidad (6 L si es el pack de leche, 12 si es
 * la docena), a su precio por unidad en cada cadena. Si no, una tienda con el paquete de 24
 * huevos parecería más cara que otra con la docena.
 */
@Service
@RequiredArgsConstructor
public class RepartoService {

    private final AccesoHogar acceso;
    private final ArticuloListaRepository articulos;
    private final GenericoRepository genericos;
    private final GenericoService precios;
    private final ClasificacionRepository clasificaciones;
    private final ProductoRepository productos;

    @Transactional(readOnly = true)
    public RepartoDto repartir(UUID hogarId, String usuarioId) {
        acceso.exigirMiembro(hogarId, usuarioId);
        List<ArticuloLista> pendientes = articulos.findByHogarIdAndCompradoFalse(hogarId);
        Map<UUID, Generico> delHogar = genericos.delHogar(hogarId).stream()
                .collect(Collectors.toMap(Generico::getId, Function.identity()));
        Map<UUID, List<PrecioTiendaDto>> porGenerico = precios.preciosPorCadena(hogarId);
        Map<String, UUID> deLosTickets = genericosDeLosTickets(hogarId);
        Map<String, Producto> escaneados = new HashMap<>();
        productos.findAllById(pendientes.stream().map(ArticuloLista::getEan).filter(e -> e != null).toList())
                .forEach(p -> escaneados.put(p.getEan(), p));

        Map<Cadena, List<RepartoDto.Articulo>> porCadena = new EnumMap<>(Cadena.class);
        List<RepartoDto.SinPrecio> sinPrecio = new ArrayList<>();
        // Para «solo en una tienda»: lo que costaría cada cosa en cada cadena.
        Map<Cadena, BigDecimal> totalEnUna = new EnumMap<>(Cadena.class);
        Map<Cadena, Integer> tieneEnUna = new EnumMap<>(Cadena.class);
        int conPrecio = 0;

        for (ArticuloLista a : pendientes) {
            Generico g = generico(a, delHogar, deLosTickets, escaneados).orElse(null);
            List<PrecioTiendaDto> suyos = g == null ? List.of() : porGenerico.getOrDefault(g.getId(), List.of());
            if (suyos.isEmpty()) {
                sinPrecio.add(new RepartoDto.SinPrecio(a.getId(), a.getNombre(), g == null ? null : g.getNombre()));
                continue;
            }
            conPrecio++;
            PrecioTiendaDto mejor = suyos.getFirst();
            BigDecimal cuanto = (mejor.tamano() == null ? BigDecimal.ONE : mejor.tamano())
                    .multiply(BigDecimal.valueOf(a.getCantidad()));
            for (PrecioTiendaDto p : suyos) {
                totalEnUna.merge(p.cadena(), coste(p, cuanto), BigDecimal::add);
                tieneEnUna.merge(p.cadena(), 1, Integer::sum);
            }
            porCadena.computeIfAbsent(mejor.cadena(), c -> new ArrayList<>()).add(new RepartoDto.Articulo(
                    a.getId(), a.getNombre(), a.getCantidad(), g.getId(), g.getNombre(), g.getCategoria(), g.getRetrato(),
                    mejor.nombre(), mejor.precio(), g.getUnidad(), coste(mejor, cuanto), mejor.fuente()));
        }

        List<RepartoDto.Grupo> grupos = porCadena.entrySet().stream()
                .map(e -> new RepartoDto.Grupo(e.getKey(), suma(e.getValue()), e.getValue()))
                .sorted(Comparator.comparing((RepartoDto.Grupo gr) -> gr.articulos().size()).reversed()
                        .thenComparing(RepartoDto.Grupo::total, Comparator.reverseOrder()))
                .toList();
        BigDecimal total = grupos.stream().map(RepartoDto.Grupo::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        int conPrecioFinal = conPrecio;
        List<RepartoDto.EnUna> enUna = totalEnUna.entrySet().stream()
                .map(e -> new RepartoDto.EnUna(e.getKey(), e.getValue(), conPrecioFinal - tieneEnUna.get(e.getKey())))
                .sorted(Comparator.comparingInt(RepartoDto.EnUna::faltan).thenComparing(RepartoDto.EnUna::total))
                .toList();
        return new RepartoDto(grupos, sinPrecio, total, enUna);
    }

    private static Optional<Generico> generico(ArticuloLista a, Map<UUID, Generico> delHogar,
                                               Map<String, UUID> deLosTickets, Map<String, Producto> escaneados) {
        UUID visto = deLosTickets.get(Textos.normalizar(a.getNombre()));
        if (visto != null && delHogar.containsKey(visto)) {
            return Optional.of(delHogar.get(visto));
        }
        Producto p = a.getEan() == null ? null : escaneados.get(a.getEan());
        return Adivinador.adivinar(p == null ? null : p.getCategorias(),
                Arrays.asList(p == null ? null : p.getNombre(), a.getNombre()), delHogar.values());
    }

    /** Nombre casado en los tickets (normalizado) → su genérico, según la clasificación de esa línea. */
    private Map<String, UUID> genericosDeLosTickets(UUID hogarId) {
        Map<Clasificacion.Clave, UUID> clasificadas = new HashMap<>();
        for (Clasificacion c : clasificaciones.findByClaveHogarId(hogarId)) {
            if (c.getGenericoId() != null) {
                clasificadas.put(c.getClave(), c.getGenericoId());
            }
        }
        Map<String, UUID> porNombre = new HashMap<>();
        for (Compra c : clasificaciones.compras(hogarId, LocalDate.now().minusYears(1))) {
            UUID g = c.nombre() == null ? null : clasificadas.get(c.clave(hogarId));
            if (g != null) {
                // Vienen de la más nueva a la más vieja: manda la última vez.
                porNombre.putIfAbsent(Textos.normalizar(c.nombre()), g);
            }
        }
        return porNombre;
    }

    /** Lo que cuesta esa cantidad (en kilos, litros o piezas) al precio por unidad de esa cadena. */
    private static BigDecimal coste(PrecioTiendaDto p, BigDecimal cuanto) {
        return p.precio().multiply(cuanto).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static BigDecimal suma(List<RepartoDto.Articulo> articulos) {
        return articulos.stream().map(RepartoDto.Articulo::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
