package dev.pimon.listillo.tienda;

import dev.pimon.common.exception.AppException;
import dev.pimon.listillo.hogar.AccesoHogar;
import dev.pimon.listillo.ticket.Cadena;
import dev.pimon.listillo.ticket.Textos;
import dev.pimon.listillo.tienda.dto.BusquedaDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Buscar cualquier cosa en lo que publican las webs de los súper, tenga genérico o no
 * («fregona», «pilas aaa»): lo más barato de cada cadena, de la más barata a la más cara.
 *
 * Se compara por el precio del envase. Si todo lo encontrado se mide igual (todo en kilos,
 * todo en litros), por el precio por unidad, que es lo justo entre envases distintos.
 */
@Service
@RequiredArgsConstructor
public class BuscadorService {

    static final int MINIMO = 3;
    static final int MAXIMO_RESULTADOS = 400;
    static final int POR_CADENA = 5;

    private final AccesoHogar acceso;
    private final ProductoTiendaRepository productos;
    private final dev.pimon.listillo.hogar.CadenasDelHogar cadenasDelHogar;
    private final RarezaDePalabras rareza;

    @Transactional(readOnly = true)
    public BusquedaDto buscar(UUID hogarId, String usuarioId, String texto) {
        acceso.exigirMiembro(hogarId, usuarioId);
        List<String> palabras = Textos.palabras(texto == null ? "" : texto);
        if (palabras.isEmpty() || String.join("", palabras).length() < MINIMO) {
            throw AppException.badRequest("Escribe al menos " + MINIMO + " letras");
        }
        String[] patrones = new String[4];
        for (int i = 0; i < 4; i++) {
            patrones[i] = i < palabras.size() ? "%" + singular(palabras.get(i)) + "%" : "%";
        }
        java.util.Set<Cadena> visibles = cadenasDelHogar.de(hogarId);
        List<ProductoTienda> encontrados = productos.buscar(patrones[0], patrones[1], patrones[2], patrones[3],
                PageRequest.of(0, MAXIMO_RESULTADOS)).stream()
                .filter(p -> visibles.contains(p.getClave().getCadena())).toList();

        boolean mismaMedida = encontrados.stream().map(ProductoTienda::getUnidad).distinct().count() == 1;
        Comparator<ProductoTienda> orden = mismaMedida
                ? Comparator.comparing(ProductoTienda::getPrecioUnidad)
                : Comparator.comparing(ProductoTienda::getPrecio);

        Map<Cadena, List<ProductoTienda>> porCadena = new EnumMap<>(Cadena.class);
        for (ProductoTienda p : encontrados) {
            porCadena.computeIfAbsent(p.getClave().getCadena(), c -> new ArrayList<>()).add(p);
        }
        List<BusquedaDto.Tienda> tiendas = porCadena.entrySet().stream()
                .map(e -> {
                    List<ProductoTienda> suyos = e.getValue().stream().sorted(orden).toList();
                    return new BusquedaDto.Tienda(e.getKey(), suyos.size(),
                            suyos.stream().limit(POR_CADENA).map(BuscadorService::aDto).toList());
                })
                .sorted(Comparator.comparing(t -> mismaMedida ? t.productos().getFirst().precioUnidad()
                        : t.productos().getFirst().precio()))
                .toList();
        return new BusquedaDto(texto.strip(), mismaMedida, tiendas);
    }

    /**
     * Lo mismo en otro súper: el 70 % del peso de las palabras en común. Las raras (la marca)
     * pesan más: «Coca-Cola zero azúcar zero cafeína» no es la Pepsi del mismo nombre.
     */
    static final double PARECIDO_MINIMO = 0.7;
    /** «El mismo tamaño» con un margen: 1 L y 1,05 L, o un «aprox» al peso. */
    static final double MARGEN_TAMANO = 0.15;
    private static final int CANDIDATOS = 3000;
    /** Palabras del nombre que no dicen qué producto es: el envase, la unidad, la oferta. */
    private static final java.util.Set<String> RUIDO = java.util.Set.of(
            "refresco", "lata", "latas", "botella", "botellas", "brik", "briks", "pack", "bandeja", "bolsa",
            "tarro", "frasco", "bote", "caja", "paquete", "unidad", "unidades", "ud", "uds", "un", "oferta",
            "aprox", "aproximadamente", "cl", "ml", "kg", "gr", "lt", "cc", "por", "para", "pieza", "piezas");

    /**
     * Ese producto en cada súper (las cadenas que compara la casa): en cada cadena, el que más
     * se le parece por el nombre, con la misma unidad y, si lo hay, el mismo tamaño. Si no lo
     * hay del mismo tamaño, otro envase, comparado por precio por unidad.
     */
    @Transactional(readOnly = true)
    public dev.pimon.listillo.tienda.dto.EnCadaSuperDto enCadaSuper(UUID hogarId, String usuarioId, Cadena cadena,
                                                                   String idExterno) {
        acceso.exigirMiembro(hogarId, usuarioId);
        ProductoTienda elegido = productos.findById(new ProductoTienda.Clave(cadena, idExterno))
                .orElseThrow(() -> AppException.notFound("Ese producto ya no está en la web"));
        java.util.Set<Cadena> visibles = cadenasDelHogar.de(hogarId);
        List<String> suyas = significativas(elegido.getNombre());

        // Candidatos: los que llevan alguna de sus dos palabras más raras (las que más dicen;
        // suele estar la marca). Una sola se queda corta: Consum no pone «cafeína».
        List<String> raras = suyas.stream().sorted(Comparator.comparingDouble(rareza::peso).reversed()).limit(2).toList();
        Map<ProductoTienda.Clave, ProductoTienda> candidatos = new java.util.LinkedHashMap<>();
        for (String rara : raras) {
            productos.buscar("%" + rara + "%", "%", "%", "%", PageRequest.of(0, CANDIDATOS))
                    .forEach(p -> candidatos.putIfAbsent(p.getClave(), p));
        }
        Map<Cadena, ProductoTienda> mejor = new EnumMap<>(Cadena.class);
        Map<Cadena, Double> notaMejor = new EnumMap<>(Cadena.class);
        Map<Cadena, Boolean> mismoTamano = new EnumMap<>(Cadena.class);
        for (ProductoTienda p : candidatos.values()) {
            Cadena c = p.getClave().getCadena();
            if (!visibles.contains(c) || p.getUnidad() != elegido.getUnidad() || c == cadena) {
                continue;
            }
            double nota = parecido(suyas, significativas(p.getNombre()), rareza::peso);
            if (nota < PARECIDO_MINIMO) {
                continue;
            }
            boolean igual = mismoTamano(elegido.getTamano(), p.getTamano());
            // Gana el del mismo tamaño; luego el que más se parece; luego el más barato.
            ProductoTienda actual = mejor.get(c);
            boolean gana = actual == null
                    || (igual && !mismoTamano.get(c))
                    || (igual == mismoTamano.get(c) && (nota > notaMejor.get(c) + 1e-9
                    || (Math.abs(nota - notaMejor.get(c)) <= 1e-9 && p.getPrecioUnidad().compareTo(actual.getPrecioUnidad()) < 0)));
            if (gana) {
                mejor.put(c, p);
                notaMejor.put(c, nota);
                mismoTamano.put(c, igual);
            }
        }
        List<dev.pimon.listillo.tienda.dto.EnCadaSuperDto.Tienda> tiendas = new ArrayList<>();
        tiendas.add(new dev.pimon.listillo.tienda.dto.EnCadaSuperDto.Tienda(cadena, aDto(elegido), true));
        mejor.forEach((c, p) -> tiendas.add(new dev.pimon.listillo.tienda.dto.EnCadaSuperDto.Tienda(c, aDto(p),
                mismoTamano.get(c))));
        tiendas.sort(Comparator.comparing(t -> t.producto().precioUnidad()));
        return new dev.pimon.listillo.tienda.dto.EnCadaSuperDto(cadena, aDto(elegido), tiendas);
    }

    /** Las palabras que dicen qué producto es: sin cifras, sin envases ni unidades, en singular. */
    static List<String> significativas(String nombre) {
        return Textos.palabras(nombre).stream()
                .filter(p -> p.chars().noneMatch(Character::isDigit) && !RUIDO.contains(p))
                .map(BuscadorService::singular)
                .distinct()
                .toList();
    }

    /** El peso de las palabras que comparten sobre el de todas las que hay entre los dos (Jaccard con pesos). */
    static double parecido(List<String> una, List<String> otra, java.util.function.ToDoubleFunction<String> peso) {
        if (una.isEmpty() || otra.isEmpty()) {
            return 0;
        }
        java.util.Set<String> comunes = new java.util.HashSet<>(una);
        comunes.retainAll(otra);
        java.util.Set<String> todas = new java.util.HashSet<>(una);
        todas.addAll(otra);
        double arriba = comunes.stream().mapToDouble(peso).sum();
        double abajo = todas.stream().mapToDouble(peso).sum();
        return abajo == 0 ? 0 : arriba / abajo;
    }

    static boolean mismoTamano(java.math.BigDecimal uno, java.math.BigDecimal otro) {
        double x = uno.doubleValue();
        double y = otro.doubleValue();
        return Math.abs(x - y) <= MARGEN_TAMANO * Math.max(x, y);
    }

    /**
     * «pilas» busca «pila» (y así encuentra «Pila alcalina» y «Pilas alcalinas»); «bayetas»,
     * «bayeta». Solo la «s» final de palabras largas: «aaa» o «gas» se quedan como están.
     */
    public static String singular(String palabra) {
        return palabra.length() > 4 && palabra.endsWith("s") ? palabra.substring(0, palabra.length() - 1) : palabra;
    }

    private static BusquedaDto.Producto aDto(ProductoTienda p) {
        return new BusquedaDto.Producto(p.getClave().getIdExterno(), p.getNombre(), p.getFormato(), p.getPrecio(),
                p.getTamano(), p.getUnidad(), p.getPrecioUnidad(), p.getImagenUrl(), p.getUrl(), p.fecha(),
                p.getFuente());
    }
}
