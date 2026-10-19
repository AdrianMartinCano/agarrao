package dev.pimon.listillo.generico;

import dev.pimon.common.exception.AppException;
import dev.pimon.listillo.generico.dto.ClasificacionLineaDto;
import dev.pimon.listillo.generico.dto.ClasificarRequest;
import dev.pimon.listillo.generico.dto.ClasificarWebRequest;
import dev.pimon.listillo.generico.dto.GenericoDetalleDto;
import dev.pimon.listillo.generico.dto.GenericoDto;
import dev.pimon.listillo.generico.dto.GenericoResumenDto;
import dev.pimon.listillo.generico.dto.NuevoGenericoRequest;
import dev.pimon.listillo.generico.dto.PrecioTiendaDto;
import dev.pimon.listillo.generico.dto.ProductoDeGenericoDto;
import dev.pimon.listillo.hogar.AccesoHogar;
import dev.pimon.listillo.generico.dto.ProductoWebDto;
import dev.pimon.listillo.producto.Producto;
import dev.pimon.listillo.ticket.Cadena;
import dev.pimon.listillo.ticket.Textos;
import dev.pimon.listillo.tienda.ProductoTienda;
import dev.pimon.listillo.tienda.ProductoTiendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Los genéricos de un hogar y su comparación por precio por unidad, sacada de sus tickets.
 *
 * Cada producto (el texto del ticket en su cadena) cuenta con su ÚLTIMO precio: si el
 * Hacendado subió en septiembre, compara lo de septiembre, no la media del año.
 */
@Service
@RequiredArgsConstructor
public class GenericoService {

    /** Los precios más viejos ya no dicen nada de lo que cuesta hoy. */
    static final int DIAS_DE_PRECIOS = 180;
    static final int MAXIMO_PROPIOS = 100;
    /** En el detalle, cuántos productos de la web se enseñan (los más baratos). */
    static final int PRODUCTOS_WEB = 24;
    /** Tope de productos de las webs que se buscan para un genérico propio. */
    private static final int WEB_POR_PROPIO = 300;

    private static final Comparator<BigDecimal> PRECIO_SIN_DATO_AL_FINAL =
            Comparator.nullsLast(Comparator.naturalOrder());

    private final AccesoHogar acceso;
    private final GenericoRepository genericos;
    private final ClasificacionRepository clasificaciones;
    private final Clasificador clasificador;
    private final ProductoTiendaRepository enLaWeb;
    private final dev.pimon.listillo.hogar.CadenasDelHogar cadenasDelHogar;
    private final DescarteWebRepository descartes;

    /**
     * Antes de contar, clasifica lo que esté pendiente: los tickets de antes de que hubiera
     * genéricos, o los que el evento de guardado no pudo clasificar.
     */
    @Transactional
    public List<GenericoResumenDto> catalogo(UUID hogarId, String usuarioId) {
        acceso.exigirMiembro(hogarId, usuarioId);
        clasificador.clasificarPendientes(hogarId);
        java.util.Set<Cadena> visibles = cadenasDelHogar.de(hogarId);
        List<Generico> todos = genericos.delHogar(hogarId);
        Map<UUID, List<ProductoTasado>> porGenerico = soloVisibles(productosTasados(hogarId, todos), visibles);
        Map<UUID, List<ProductoTienda>> webPorGenerico = deLaWeb(todos, visibles);
        return todos.stream().map(g -> {
            List<ProductoTasado> suyos = porGenerico.getOrDefault(g.getId(), List.of());
            List<ProductoTienda> web = webPorGenerico.getOrDefault(g.getId(), List.of());
            PrecioTiendaDto mejor = mejoresPorCadena(suyos, web).stream().findFirst().orElse(null);
            LocalDate ultima = suyos.stream().map(p -> p.ultima().fecha()).max(Comparator.naturalOrder()).orElse(null);
            return new GenericoResumenDto(g.getId(), g.getNombre(), g.getCategoria(), g.getUnidad(), g.getRetrato(),
                    g.propio(), mejor == null ? null : mejor.precio(), mejor == null ? null : mejor.cadena(),
                    mejor == null ? null : mejor.fuente(), suyos.size(), web.size(), ultima);
        }).toList();
    }

    /**
     * Lo más barato de cada cadena para cada genérico del hogar (web o tickets), de más barata
     * a más cara. Para repartir la lista. Sin comprobar acceso: lo hace quien llama.
     */
    @Transactional(readOnly = true)
    public Map<UUID, List<PrecioTiendaDto>> preciosPorCadena(UUID hogarId) {
        java.util.Set<Cadena> visibles = cadenasDelHogar.de(hogarId);
        List<Generico> todos = genericos.delHogar(hogarId);
        Map<UUID, List<ProductoTasado>> porGenerico = soloVisibles(productosTasados(hogarId, todos), visibles);
        Map<UUID, List<ProductoTienda>> webPorGenerico = deLaWeb(todos, visibles);
        Map<UUID, List<PrecioTiendaDto>> precios = new java.util.HashMap<>();
        for (Generico g : todos) {
            List<PrecioTiendaDto> suyos = mejoresPorCadena(porGenerico.getOrDefault(g.getId(), List.of()),
                    webPorGenerico.getOrDefault(g.getId(), List.of()));
            if (!suyos.isEmpty()) {
                precios.put(g.getId(), suyos);
            }
        }
        return precios;
    }

    @Transactional(readOnly = true)
    public GenericoDetalleDto detalle(UUID hogarId, UUID genericoId, String usuarioId) {
        acceso.exigirMiembro(hogarId, usuarioId);
        Generico g = buscar(hogarId, genericoId);
        java.util.Set<Cadena> visibles = cadenasDelHogar.de(hogarId);
        List<ProductoTasado> suyos = soloVisibles(productosTasados(hogarId, List.of(g)), visibles)
                .getOrDefault(g.getId(), List.of());

        List<ProductoTienda> web = deLaWeb(List.of(g), visibles).getOrDefault(g.getId(), List.of());
        List<PrecioTiendaDto> tiendas = mejoresPorCadena(suyos, web);
        // El más barato de cada cadena primero (que ninguna quede escondida detrás de las
        // variantes de otra) y después el resto, de barato a caro.
        java.util.Set<Cadena> vistas = java.util.EnumSet.noneOf(Cadena.class);
        List<ProductoTienda> porPrecio = web.stream().sorted(Comparator.comparing(ProductoTienda::getPrecioUnidad)).toList();
        List<ProductoTienda> primeros = porPrecio.stream().filter(p -> vistas.add(p.getClave().getCadena())).toList();
        List<ProductoTienda> ordenados = new ArrayList<>(primeros);
        porPrecio.stream().filter(p -> !primeros.contains(p)).forEach(ordenados::add);
        List<ProductoWebDto> deLaWeb = ordenados.stream()
                .limit(PRODUCTOS_WEB)
                .map(p -> new ProductoWebDto(p.getClave().getCadena(), p.getClave().getIdExterno(), p.getNombre(),
                        p.getFormato(), p.getPrecio(), p.getTamano(), p.getPrecioUnidad(), p.getImagenUrl(), p.getUrl(),
                        p.fecha(), p.getFuente()))
                .toList();
        List<ProductoDeGenericoDto> productos = suyos.stream()
                .sorted(Comparator.comparing(ProductoTasado::precio, PRECIO_SIN_DATO_AL_FINAL))
                .map(p -> new ProductoDeGenericoDto(p.ultima().cadena(), p.ultima().texto(), p.nombre(), p.marca(),
                        p.tamano(), p.precio(), p.ultimaConPrecio().importe(), p.ultimaConPrecio().fecha(), p.compras(),
                        p.clasificacion().getOrigen()))
                .toList();
        return new GenericoDetalleDto(g.getId(), g.getNombre(), g.getCategoria(), g.getUnidad(), g.getRetrato(),
                g.propio(), tiendas, productos, deLaWeb);
    }

    /**
     * Lo más barato de cada cadena, de más barata a más cara. Si la cadena tiene catálogo
     * web, manda la web (es el precio de hoy); si no, el último precio de los tickets.
     */
    private static List<PrecioTiendaDto> mejoresPorCadena(List<ProductoTasado> suyos, List<ProductoTienda> web) {
        Map<Cadena, PrecioTiendaDto> mejor = new EnumMap<>(Cadena.class);
        for (ProductoTasado p : suyos) {
            if (p.precio() != null) {
                PrecioTiendaDto dto = new PrecioTiendaDto(p.ultima().cadena(), p.precio(), p.ultimaConPrecio().fecha(),
                        FuentePrecio.TICKET, p.nombre(), p.tamano(), p.ultimaConPrecio().importe(), null, null);
                mejor.merge(dto.cadena(), dto, (a, b) -> a.precio().compareTo(b.precio()) <= 0 ? a : b);
            }
        }
        Map<Cadena, PrecioTiendaDto> deLaWeb = new EnumMap<>(Cadena.class);
        for (ProductoTienda p : web) {
            PrecioTiendaDto dto = new PrecioTiendaDto(p.getClave().getCadena(), p.getPrecioUnidad(),
                    p.fecha(), p.getFuente(), p.getNombre(), p.getTamano(), p.getPrecio(),
                    p.getUrl(), p.getImagenUrl());
            deLaWeb.merge(dto.cadena(), dto, (a, b) -> a.precio().compareTo(b.precio()) <= 0 ? a : b);
        }
        // La web es de hoy y manda; Open Prices, solo si es más reciente que vuestro ticket.
        deLaWeb.forEach((cadena, dto) -> mejor.merge(cadena, dto, (ticket, deFuera) ->
                deFuera.fuente() == FuentePrecio.WEB || deFuera.fecha().isAfter(ticket.fecha()) ? deFuera : ticket));
        return mejor.values().stream().sorted(Comparator.comparing(PrecioTiendaDto::precio)).toList();
    }

    /** El genérico de cada línea de un ticket, para enseñarlo y corregirlo desde el ticket. */
    @Transactional(readOnly = true)
    public List<ClasificacionLineaDto> deTicket(UUID hogarId, UUID ticketId, String usuarioId) {
        acceso.exigirMiembro(hogarId, usuarioId);
        List<Compra> compras = clasificaciones.deTicket(hogarId, ticketId);
        if (compras.isEmpty()) {
            throw AppException.notFound("Ese ticket no existe");
        }
        Map<Clasificacion.Clave, Clasificacion> delHogar = clasificacionesDelHogar(hogarId);
        Map<UUID, Generico> porId = genericos.delHogar(hogarId).stream()
                .collect(Collectors.toMap(Generico::getId, Function.identity()));
        Map<String, Producto> productos = clasificador.productos(compras.stream().map(Compra::ean).toList());

        return compras.stream().map(c -> {
            Clasificacion cl = delHogar.get(c.clave(hogarId));
            Generico g = cl == null || cl.getGenericoId() == null ? null : porId.get(cl.getGenericoId());
            if (g == null) {
                return new ClasificacionLineaDto(c.lineaId(), null, null, null, null, null, null, null,
                        cl == null ? null : cl.getOrigen());
            }
            BigDecimal tamano = Tasador.tamano(c, cl, productos.get(c.ean()), g).orElse(null);
            return new ClasificacionLineaDto(c.lineaId(), g.getId(), g.getNombre(), g.getCategoria(), g.getUnidad(),
                    g.getRetrato(), tamano, Tasador.porUnidad(c, tamano).orElse(null), cl.getOrigen());
        }).toList();
    }

    /** Una persona dice qué genérico es (o que ninguno) y, si quiere, cuánto trae. */
    @Transactional
    public void clasificar(UUID hogarId, String usuarioId, ClasificarRequest peticion) {
        acceso.exigirMiembro(hogarId, usuarioId);
        String texto = Textos.normalizar(peticion.texto());
        if (texto.isEmpty()) {
            throw AppException.badRequest("Ese texto no dice nada");
        }
        if (peticion.genericoId() != null) {
            buscar(hogarId, peticion.genericoId());
        }
        Clasificacion.Clave clave = new Clasificacion.Clave(hogarId, peticion.cadena(), texto);
        Clasificacion c = clasificaciones.findById(clave).orElseGet(() -> Clasificacion.aMano(clave));
        c.corregir(peticion.genericoId(), peticion.genericoId() == null ? null : peticion.tamano());
        clasificaciones.save(c);
    }

    /** Corrige un producto del catálogo web (común a todos): solo con genéricos de Listillo. */
    @Transactional
    public void clasificarWeb(UUID hogarId, String usuarioId, ClasificarWebRequest peticion) {
        acceso.exigirMiembro(hogarId, usuarioId);
        // «No es esto» desde un genérico propio: solo para ese genérico de esta casa.
        if (peticion.genericoId() == null && peticion.quitarDe() != null && buscar(hogarId, peticion.quitarDe()).propio()) {
            descartes.save(new DescarteWeb(peticion.quitarDe(), peticion.cadena(), peticion.idExterno()));
            return;
        }
        if (peticion.genericoId() != null && buscar(hogarId, peticion.genericoId()).propio()) {
            throw AppException.badRequest("Los productos de las webs solo van con genéricos de Listillo");
        }
        ProductoTienda p = enLaWeb.findById(new ProductoTienda.Clave(peticion.cadena(), peticion.idExterno()))
                .orElseThrow(() -> AppException.notFound("Ese producto ya no está en la web"));
        p.corregir(peticion.genericoId());
    }

    @Transactional
    public GenericoDto crear(UUID hogarId, String usuarioId, NuevoGenericoRequest peticion) {
        acceso.exigirMiembro(hogarId, usuarioId);
        String nombre = peticion.nombre().strip();
        if (genericos.countByHogarId(hogarId) >= MAXIMO_PROPIOS) {
            throw AppException.badRequest("Ya tenéis " + MAXIMO_PROPIOS + " genéricos propios");
        }
        if (genericos.existeNombre(hogarId, nombre)) {
            throw AppException.conflict("Ya hay un genérico que se llama «" + nombre + "»");
        }
        Generico g = genericos.save(new Generico(hogarId, nombre, peticion.categoria(), peticion.unidad()));
        // Lo que no tenía genérico puede ser este.
        clasificador.clasificarPendientes(hogarId);
        return new GenericoDto(g.getId(), g.getNombre(), g.getCategoria(), g.getUnidad(), g.getRetrato(), true);
    }

    /** Solo los propios. Sus productos se quedan sin genérico y se vuelven a adivinar. */
    @Transactional
    public void borrar(UUID hogarId, UUID genericoId, String usuarioId) {
        acceso.exigirMiembro(hogarId, usuarioId);
        Generico g = buscar(hogarId, genericoId);
        if (!g.propio()) {
            throw AppException.forbidden("Los genéricos de Listillo no se pueden borrar");
        }
        genericos.delete(g);
        genericos.flush();
        clasificador.clasificarPendientes(hogarId);
    }

    /**
     * Lo de las webs de cada genérico, solo de las cadenas que compara la casa. Los del
     * catálogo, por la clasificación de cada producto; los propios, buscando su nombre (como
     * el buscador), con la misma unidad y sin lo que la casa ha descartado.
     */
    private Map<UUID, List<ProductoTienda>> deLaWeb(List<Generico> todos, java.util.Set<Cadena> visibles) {
        Map<UUID, List<ProductoTienda>> porGenerico = new java.util.HashMap<>(enLaWeb.findByGenericoIdIn(
                todos.stream().filter(g -> !g.propio()).map(Generico::getId).toList()).stream()
                .filter(p -> visibles.contains(p.getClave().getCadena()))
                .collect(Collectors.groupingBy(ProductoTienda::getGenericoId)));
        List<Generico> propios = todos.stream().filter(Generico::propio).toList();
        if (propios.isEmpty()) {
            return porGenerico;
        }
        Map<UUID, java.util.Set<ProductoTienda.Clave>> descartados = descartes.findByClaveGenericoIdIn(
                propios.stream().map(Generico::getId).toList()).stream()
                .collect(Collectors.groupingBy(d -> d.getClave().getGenericoId(), Collectors.mapping(
                        d -> new ProductoTienda.Clave(d.getClave().getCadena(), d.getClave().getIdExterno()),
                        Collectors.toSet())));
        for (Generico g : propios) {
            java.util.Set<ProductoTienda.Clave> fuera = descartados.getOrDefault(g.getId(), java.util.Set.of());
            List<ProductoTienda> suyos = porNombre(g.getNombre()).stream()
                    .filter(p -> p.getUnidad() == g.getUnidad() && visibles.contains(p.getClave().getCadena())
                            && !fuera.contains(p.getClave()))
                    .toList();
            if (!suyos.isEmpty()) {
                porGenerico.put(g.getId(), suyos);
            }
        }
        return porGenerico;
    }

    /** Lo de las webs que se llama así: todas las palabras del nombre (hasta cuatro). */
    private List<ProductoTienda> porNombre(String nombre) {
        List<String> palabras = Textos.palabras(nombre).stream()
                .filter(p -> p.chars().noneMatch(Character::isDigit)).limit(4).toList();
        if (String.join("", palabras).length() < 3) {
            return List.of();
        }
        String[] patrones = new String[4];
        for (int i = 0; i < 4; i++) {
            patrones[i] = i < palabras.size()
                    ? "%" + dev.pimon.listillo.tienda.BuscadorService.singular(palabras.get(i)) + "%" : "%";
        }
        return enLaWeb.buscar(patrones[0], patrones[1], patrones[2], patrones[3],
                org.springframework.data.domain.PageRequest.of(0, WEB_POR_PROPIO));
    }

    /** Lo comprado solo en las cadenas que la casa compara (Ajustes). */
    private static Map<UUID, List<ProductoTasado>> soloVisibles(Map<UUID, List<ProductoTasado>> porGenerico,
                                                                java.util.Set<Cadena> visibles) {
        Map<UUID, List<ProductoTasado>> filtrado = new LinkedHashMap<>();
        porGenerico.forEach((id, lista) -> {
            List<ProductoTasado> suyos = lista.stream().filter(p -> visibles.contains(p.ultima().cadena())).toList();
            if (!suyos.isEmpty()) {
                filtrado.put(id, suyos);
            }
        });
        return filtrado;
    }

    private Generico buscar(UUID hogarId, UUID genericoId) {
        return genericos.visible(genericoId, hogarId)
                .orElseThrow(() -> AppException.notFound("Ese genérico no existe"));
    }

    private Map<Clasificacion.Clave, Clasificacion> clasificacionesDelHogar(UUID hogarId) {
        return clasificaciones.findByClaveHogarId(hogarId).stream()
                .collect(Collectors.toMap(Clasificacion::getClave, Function.identity()));
    }

    /** Los productos comprados de estos genéricos en los últimos meses, con su último precio. */
    private Map<UUID, List<ProductoTasado>> productosTasados(UUID hogarId, List<Generico> deEstos) {
        Map<UUID, Generico> porId = deEstos.stream().collect(Collectors.toMap(Generico::getId, Function.identity()));
        Map<Clasificacion.Clave, Clasificacion> delHogar = clasificacionesDelHogar(hogarId);

        // Las compras de cada producto, de la más nueva a la más vieja (así llegan).
        Map<Clasificacion.Clave, List<Compra>> porProducto = new LinkedHashMap<>();
        for (Compra c : clasificaciones.compras(hogarId, LocalDate.now().minusDays(DIAS_DE_PRECIOS))) {
            Clasificacion cl = delHogar.get(c.clave(hogarId));
            if (cl != null && cl.getGenericoId() != null && porId.containsKey(cl.getGenericoId())) {
                porProducto.computeIfAbsent(cl.getClave(), k -> new ArrayList<>()).add(c);
            }
        }
        Map<String, Producto> productos = clasificador.productos(
                porProducto.values().stream().flatMap(List::stream).map(Compra::ean).toList());

        Map<UUID, List<ProductoTasado>> resultado = new LinkedHashMap<>();
        porProducto.forEach((clave, compras) -> {
            Clasificacion cl = delHogar.get(clave);
            Generico g = porId.get(cl.getGenericoId());
            ProductoTasado tasado = tasar(compras, cl, g, productos);
            resultado.computeIfAbsent(g.getId(), k -> new ArrayList<>()).add(tasado);
        });
        return resultado;
    }

    private static ProductoTasado tasar(List<Compra> compras, Clasificacion cl, Generico g, Map<String, Producto> productos) {
        Compra ultima = compras.getFirst();
        // El último precio que se pueda calcular: un descuento suelto no deja el producto sin precio.
        Compra conPrecio = null;
        BigDecimal tamano = null;
        BigDecimal precio = null;
        for (Compra c : compras) {
            BigDecimal t = Tasador.tamano(c, cl, productos.get(c.ean()), g).orElse(null);
            BigDecimal p = Tasador.porUnidad(c, t).orElse(null);
            if (p != null) {
                conPrecio = c;
                tamano = t;
                precio = p;
                break;
            }
        }
        Producto producto = compras.stream().map(Compra::ean).filter(e -> e != null && productos.containsKey(e))
                .map(productos::get).findFirst().orElse(null);
        String nombre = producto != null && producto.getNombre() != null ? producto.getNombre()
                : compras.stream().map(Compra::nombre).filter(n -> n != null).findFirst().orElse(ultima.texto());
        return new ProductoTasado(cl, ultima, conPrecio == null ? ultima : conPrecio, nombre,
                producto == null ? null : producto.getMarca(), tamano, precio, compras.size());
    }

    /** Un producto con su última compra y su último precio por unidad (si se puede calcular). */
    private record ProductoTasado(Clasificacion clasificacion, Compra ultima, Compra ultimaConPrecio, String nombre,
                                  String marca, BigDecimal tamano, BigDecimal precio, int compras) {
    }
}
