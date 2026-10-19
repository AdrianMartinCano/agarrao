package dev.pimon.listillo.tienda;

import dev.pimon.listillo.generico.Adivinador;
import dev.pimon.listillo.generico.FuentePrecio;
import dev.pimon.listillo.generico.Generico;
import dev.pimon.listillo.generico.GenericoRepository;
import dev.pimon.listillo.ticket.Cadena;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Importa los precios de España de Open Prices a {@code productos_tienda}, con
 * {@code fuente = OPEN_PRICES} y la fecha del precio. Cada noche y, al arrancar, si hace más
 * de 20 horas de la última vez.
 *
 * Solo para las cadenas cuya web no se lee: con {@code PRECIOS_WEB} encendido, las que no
 * tienen {@link CatalogoWeb} (Carrefour, Dia, Alcampo…); apagado, todas. De cada producto en
 * cada cadena se queda el precio más reciente. El genérico se adivina por la categoría de
 * Open Food Facts y, si no, por la primera palabra del nombre (como el de las webs).
 */
@Service
public class ImportadorOpenPrices {

    private static final Logger log = LoggerFactory.getLogger(ImportadorOpenPrices.class);

    private final OpenPrices api;
    private final List<CatalogoWeb> catalogos;
    private final PreciosWebProperties web;
    private final OpenPricesProperties props;
    private final ProductoTiendaRepository productos;
    private final GenericoRepository genericos;
    private final TransactionTemplate transaccion;
    private final AtomicBoolean importando = new AtomicBoolean();

    public ImportadorOpenPrices(OpenPrices api, List<CatalogoWeb> catalogos, PreciosWebProperties web,
                                OpenPricesProperties props, ProductoTiendaRepository productos,
                                GenericoRepository genericos, TransactionTemplate transaccion) {
        this.api = api;
        this.catalogos = catalogos;
        this.web = web;
        this.props = props;
        this.productos = productos;
        this.genericos = genericos;
        this.transaccion = transaccion;
    }

    @Scheduled(cron = "${listillo.open-prices.cron:0 15 5 * * *}")
    public void cadaNoche() {
        if (props.activo()) {
            importar();
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        if (!props.activo()) {
            return;
        }
        boolean anticuado = productos.ultimaImportacion(FuentePrecio.OPEN_PRICES)
                .map(ultima -> ultima.isBefore(LocalDateTime.now().minusHours(20)))
                .orElse(true);
        if (anticuado) {
            Thread.ofVirtual().name("importar-open-prices").start(this::importar);
        }
    }

    /** Las cadenas que rellena Open Prices: las que no se leen de su web. */
    Set<Cadena> cadenas() {
        Set<Cadena> cadenas = EnumSet.allOf(Cadena.class);
        cadenas.remove(Cadena.OTRA);
        if (web.activo()) {
            catalogos.forEach(c -> cadenas.remove(c.cadena()));
        }
        return cadenas;
    }

    /** Cuántos productos se han guardado de cada cadena. Si ya hay una en marcha, nada. */
    public Map<Cadena, Integer> importar() {
        if (!importando.compareAndSet(false, true)) {
            log.info("Ya hay una importación de Open Prices en marcha");
            return Map.of();
        }
        try {
            Set<Cadena> cadenas = cadenas();
            LocalDateTime inicio = LocalDateTime.now();
            Map<Long, Cadena> locales = api.locales().entrySet().stream()
                    .filter(e -> cadenas.contains(e.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            List<OpenPrices.PrecioAbierto> leidos = locales.isEmpty() ? List.of()
                    : api.precios(locales, LocalDate.now().minusDays(props.dias()));
            if (leidos.isEmpty()) {
                // Sin nada leído (API caída) no se borra lo que había.
                log.warn("Open Prices no ha devuelto precios; se deja lo anterior");
                return Map.of();
            }
            Map<Cadena, Integer> resultado = transaccion.execute(estado -> guardar(cadenas, ultimos(leidos), inicio));
            log.info("Open Prices: {}", resultado);
            return resultado == null ? Map.of() : resultado;
        } catch (RuntimeException e) {
            log.warn("La importación de Open Prices ha fallado: {}", e.getMessage());
            return Map.of();
        } finally {
            importando.set(false);
        }
    }

    /** De cada producto en cada cadena, el precio más reciente (y a igual día, el más barato). */
    static List<OpenPrices.PrecioAbierto> ultimos(List<OpenPrices.PrecioAbierto> leidos) {
        Comparator<OpenPrices.PrecioAbierto> mejor = Comparator.comparing(OpenPrices.PrecioAbierto::fecha)
                .thenComparing(p -> p.producto().precio(), Comparator.reverseOrder());
        return List.copyOf(leidos.stream().collect(Collectors.toMap(
                p -> p.cadena() + "/" + p.producto().idExterno(), Function.identity(),
                (a, b) -> mejor.compare(a, b) >= 0 ? a : b, LinkedHashMap::new)).values());
    }

    private Map<Cadena, Integer> guardar(Set<Cadena> cadenas, List<OpenPrices.PrecioAbierto> precios,
                                         LocalDateTime inicio) {
        List<Generico> delCatalogo = genericos.findByHogarIdIsNull();
        Map<Cadena, Integer> resultado = new EnumMap<>(Cadena.class);
        Map<Cadena, List<OpenPrices.PrecioAbierto>> porCadena = precios.stream()
                .collect(Collectors.groupingBy(OpenPrices.PrecioAbierto::cadena));
        for (Cadena cadena : cadenas) {
            List<OpenPrices.PrecioAbierto> suyos = porCadena.getOrDefault(cadena, List.of());
            Map<String, ProductoTienda> existentes = productos.findByClaveCadena(cadena).stream()
                    .collect(Collectors.toMap(p -> p.getClave().getIdExterno(), Function.identity()));
            for (OpenPrices.PrecioAbierto precio : suyos) {
                ProductoWeb w = precio.producto();
                ProductoTienda p = existentes.computeIfAbsent(w.idExterno(), id -> new ProductoTienda(cadena, id));
                p.actualizar(w, FuentePrecio.OPEN_PRICES, precio.fecha());
                p.adivinado(adivinar(precio, delCatalogo));
            }
            productos.saveAll(existentes.values());
            if (!suyos.isEmpty()) {
                resultado.put(cadena, suyos.size());
            }
        }
        productos.flush();
        for (Cadena cadena : cadenas) {
            productos.borrarNoVistosDe(cadena, FuentePrecio.OPEN_PRICES, inicio);
        }
        return resultado;
    }

    private static UUID adivinar(OpenPrices.PrecioAbierto precio, List<Generico> delCatalogo) {
        ProductoWeb w = precio.producto();
        List<Generico> candidatos = delCatalogo.stream().filter(g -> g.getUnidad() == w.unidad()).toList();
        return Adivinador.adivinar(precio.categoriasOff(), List.of(), candidatos)
                .or(() -> Adivinador.adivinarPorCabeza(w.nombre(), candidatos))
                .map(Generico::getId).orElse(null);
    }
}
