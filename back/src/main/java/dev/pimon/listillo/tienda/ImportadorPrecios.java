package dev.pimon.listillo.tienda;

import dev.pimon.listillo.generico.Adivinador;
import dev.pimon.listillo.generico.Categoria;
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

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Importa los catálogos web de las cadenas: cada noche y, al arrancar, si el de alguna
 * cadena está vacío o tiene más de 20 horas. Solo con {@code listillo.precios-web.activo}.
 *
 * Leer la web va fuera de transacción (son minutos); guardar, en una por cadena. Lo que ya
 * no sale en la web se borra. El genérico se adivina por el nombre, buscando solo entre los
 * genéricos del catálogo de Listillo de los pasillos de esa sección y con la misma unidad
 * (un aceite en litros no puede ser «Arroz»); lo corregido a mano se respeta.
 */
@Service
public class ImportadorPrecios {

    private static final Logger log = LoggerFactory.getLogger(ImportadorPrecios.class);

    private final List<CatalogoWeb> catalogos;
    private final ProductoTiendaRepository productos;
    private final GenericoRepository genericos;
    private final PreciosWebProperties props;
    private final TransactionTemplate transaccion;
    private final AtomicBoolean importando = new AtomicBoolean();

    public ImportadorPrecios(List<CatalogoWeb> catalogos, ProductoTiendaRepository productos,
                             GenericoRepository genericos, PreciosWebProperties props, TransactionTemplate transaccion) {
        this.catalogos = catalogos;
        this.productos = productos;
        this.genericos = genericos;
        this.props = props;
        this.transaccion = transaccion;
    }

    @Scheduled(cron = "${listillo.precios-web.cron:0 30 4 * * *}")
    public void cadaNoche() {
        if (props.activo()) {
            importarTodo();
        }
    }

    /** Al arrancar, en otro hilo para no retrasar el arranque. */
    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        if (!props.activo()) {
            return;
        }
        Thread.ofVirtual().name("importar-precios").start(() -> {
            // Si el catálogo de genéricos ha cambiado (una migración nueva), que se note ya y
            // no a la noche siguiente.
            reclasificar();
            for (CatalogoWeb c : catalogos) {
                if (anticuado(c.cadena())) {
                    importar(c);
                }
            }
        });
    }

    /**
     * Vuelve a adivinar el genérico de lo ya importado de las webs con el catálogo de hoy, sin
     * leerlas (segundos). Lo corregido a mano no se toca. Devuelve cuántos han cambiado.
     */
    public int reclasificar() {
        int cambiados = 0;
        for (CatalogoWeb c : catalogos) {
            cambiados += reclasificar(c);
        }
        log.info("Reclasificados {} productos de las webs", cambiados);
        return cambiados;
    }

    /** Lo mismo, solo lo de una cadena. */
    public int reclasificar(CatalogoWeb catalogo) {
        try {
            Integer cambiados = transaccion.execute(estado -> {
                List<Generico> delCatalogo = genericos.findByHogarIdIsNull();
                int n = 0;
                for (ProductoTienda p : productos.findByClaveCadena(catalogo.cadena())) {
                    if (p.getFuente() != dev.pimon.listillo.generico.FuentePrecio.WEB || p.isGenericoManual()) {
                        continue;
                    }
                    UUID nuevo = adivinar(catalogo, p.getNombre(), p.getSeccion(), p.getUnidad(), delCatalogo);
                    if (!java.util.Objects.equals(nuevo, p.getGenericoId())) {
                        p.adivinado(nuevo);
                        n++;
                    }
                }
                return n;
            });
            return cambiados == null ? 0 : cambiados;
        } catch (RuntimeException e) {
            log.warn("{}: no se ha podido reclasificar: {}", catalogo.cadena(), e.getMessage());
            return 0;
        }
    }

    /** Todas las cadenas, una detrás de otra. */
    public Map<Cadena, Integer> importarTodo() {
        Map<Cadena, Integer> resultado = new EnumMap<>(Cadena.class);
        for (CatalogoWeb c : catalogos) {
            resultado.put(c.cadena(), importar(c));
        }
        return resultado;
    }

    /** Solo la de esa cadena (vacío si no tiene catálogo web). */
    public Map<Cadena, Integer> importar(Cadena cadena) {
        Map<Cadena, Integer> resultado = new EnumMap<>(Cadena.class);
        catalogos.stream().filter(c -> c.cadena() == cadena).findFirst()
                .ifPresent(c -> resultado.put(cadena, importar(c)));
        return resultado;
    }

    /** Una cadena. Si ya hay una importación en marcha, no hace nada (devuelve 0). */
    public int importar(CatalogoWeb catalogo) {
        if (!importando.compareAndSet(false, true)) {
            log.info("Ya hay una importación de precios en marcha");
            return 0;
        }
        try {
            LocalDateTime inicio = LocalDateTime.now();
            List<ProductoWeb> leidos = catalogo.leer();
            if (leidos.isEmpty()) {
                // Sin nada leído (web caída, formato cambiado) no se borra lo que había.
                log.warn("{}: la web no ha devuelto productos; se deja el catálogo anterior", catalogo.cadena());
                return 0;
            }
            Integer guardados = transaccion.execute(estado -> guardar(catalogo, leidos, inicio));
            log.info("{}: {} productos importados de su web", catalogo.cadena(), guardados);
            return guardados == null ? 0 : guardados;
        } catch (RuntimeException e) {
            log.warn("{}: la importación de precios ha fallado: {}", catalogo.cadena(), e.getMessage());
            return 0;
        } finally {
            importando.set(false);
        }
    }

    private int guardar(CatalogoWeb catalogo, List<ProductoWeb> leidos, LocalDateTime inicio) {
        Map<String, ProductoTienda> existentes = productos.findByClaveCadena(catalogo.cadena()).stream()
                .collect(Collectors.toMap(p -> p.getClave().getIdExterno(), Function.identity()));
        List<Generico> delCatalogo = genericos.findByHogarIdIsNull();
        Map<String, ProductoTienda> nuevos = new LinkedHashMap<>();
        for (ProductoWeb web : leidos) {
            ProductoTienda p = existentes.get(web.idExterno());
            if (p == null) {
                p = nuevos.computeIfAbsent(web.idExterno(), id -> new ProductoTienda(catalogo.cadena(), id));
            }
            p.actualizar(web);
            p.adivinado(adivinar(catalogo, web.nombre(), web.seccion(), web.unidad(), delCatalogo));
        }
        productos.saveAll(existentes.values());
        productos.saveAll(nuevos.values());
        productos.flush();
        productos.borrarNoVistos(catalogo.cadena(), inicio);
        return leidos.size();
    }

    private static UUID adivinar(CatalogoWeb catalogo, String nombre, String seccion,
                                 dev.pimon.listillo.ticket.Unidad unidad, List<Generico> delCatalogo) {
        Set<Categoria> pasillos = catalogo.pasillos(seccion);
        if (pasillos.isEmpty()) {
            return null;
        }
        List<Generico> candidatos = delCatalogo.stream()
                .filter(g -> pasillos.contains(g.getCategoria()) && g.getUnidad() == unidad)
                .toList();
        return Adivinador.adivinarPorCabeza(nombre, candidatos).map(Generico::getId).orElse(null);
    }

    private boolean anticuado(Cadena cadena) {
        return productos.findByClaveCadena(cadena).stream()
                .map(ProductoTienda::getActualizadoEn)
                .max(LocalDateTime::compareTo)
                .map(ultima -> ultima.isBefore(LocalDateTime.now().minusHours(20)))
                .orElse(true);
    }
}
