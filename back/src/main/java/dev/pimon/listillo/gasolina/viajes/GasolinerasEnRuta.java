package dev.pimon.listillo.gasolina.viajes;

import dev.pimon.common.exception.AppException;
import dev.pimon.listillo.gasolina.estaciones.Carburante;
import dev.pimon.listillo.gasolina.estaciones.CatalogoEstaciones;
import dev.pimon.listillo.gasolina.estaciones.Estacion;
import dev.pimon.listillo.gasolina.estaciones.EstacionMapper;
import dev.pimon.listillo.gasolina.estaciones.GasolinaProperties;
import dev.pimon.listillo.gasolina.estaciones.Geo;
import dev.pimon.listillo.gasolina.viajes.ViajeDtos.GasolineraEnRutaDto;
import dev.pimon.listillo.gasolina.viajes.ViajeDtos.ViajeDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Viajes: las gasolineras que pillan de camino entre dos sitios, de la más barata a la más
 * cara.
 *
 * «De camino» = a menos de {@code desvioKm} en línea recta de la carretera. Ir y volver
 * de la gasolinera son unos 2 × desvío de más, así que con 2 km el rodeo máximo ronda 4 km.
 *
 * Cómo se mide: para cada gasolinera cerca de la ruta se busca el tramo de la línea más
 * próximo y su distancia. Son distancias de pocos km, así que basta con aplanar la Tierra
 * alrededor de la gasolinera (grados → km con el coseno de su latitud) y hacer geometría
 * de plano; el error es de metros.
 */
@Service
public class GasolinerasEnRuta {

    static final double DESVIO_POR_DEFECTO_KM = 2;
    static final double DESVIO_MAXIMO_KM = 10;
    /** Los puntos que se mandan para pintar la ruta: más no se ven y pesan. */
    static final int PUNTOS_PARA_PINTAR = 800;

    private static final double KM_POR_GRADO = 111.32;

    private final CalculadorRutas rutas;
    private final CatalogoEstaciones catalogo;
    private final int maxEstaciones;

    public GasolinerasEnRuta(CalculadorRutas rutas, CatalogoEstaciones catalogo, GasolinaProperties props) {
        this.rutas = rutas;
        this.catalogo = catalogo;
        this.maxEstaciones = props.rutas().maxEstaciones();
    }

    public ViajeDto buscar(double origenLat, double origenLon, double destinoLat, double destinoLon,
                           Carburante carburante, Double desvio) {
        double desvioKm = desvio == null ? DESVIO_POR_DEFECTO_KM : Math.min(desvio, DESVIO_MAXIMO_KM);
        if (Geo.distanciaKm(origenLat, origenLon, destinoLat, destinoLon) < 1) {
            throw AppException.badRequest("El origen y el destino son el mismo sitio");
        }

        Ruta ruta;
        try {
            ruta = rutas.calcular(origenLat, origenLon, destinoLat, destinoLon)
                    .orElseThrow(() -> AppException.notFound("No hay ruta en coche entre esos dos sitios"));
        } catch (RutasNoDisponiblesException e) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "RUTAS_NO_DISPONIBLES",
                    "No se puede calcular la ruta ahora mismo. Prueba en un rato.");
        }

        List<GasolineraEnRutaDto> enRuta = deCamino(ruta, carburante, desvioKm);
        List<BigDecimal> precios = enRuta.stream().map(g -> g.estacion().precio()).toList();

        return new ViajeDto(
                redondear(ruta.distanciaKm(), 1),
                Math.round(ruta.duracionMin()),
                desvioKm,
                paraPintar(ruta.puntos()),
                enRuta.size(),
                media(precios),
                precios.stream().min(Comparator.naturalOrder()).orElse(null),
                enRuta.stream().limit(maxEstaciones).toList());
    }

    /** Todas las de camino con precio de ese carburante, de la más barata a la más cara. */
    List<GasolineraEnRutaDto> deCamino(Ruta ruta, Carburante carburante, double desvioKm) {
        List<double[]> puntos = ruta.puntos();
        if (puntos.size() < 2) {
            return List.of();
        }
        double[] acumulado = acumulados(puntos);
        double[] caja = caja(puntos, desvioKm);

        List<GasolineraEnRutaDto> encontradas = new ArrayList<>();
        for (Estacion e : catalogo.todas()) {
            BigDecimal precio = e.precios().get(carburante);
            if (precio == null
                    || e.latitud() < caja[0] || e.latitud() > caja[1]
                    || e.longitud() < caja[2] || e.longitud() > caja[3]) {
                continue;
            }
            double[] cerca = masCercano(e.latitud(), e.longitud(), puntos, acumulado, desvioKm);
            if (cerca != null) {
                encontradas.add(new GasolineraEnRutaDto(
                        EstacionMapper.aDto(e, null, carburante),
                        redondear(cerca[1], 1),
                        redondear(cerca[0], 2)));
            }
        }
        encontradas.sort(Comparator
                .comparing((GasolineraEnRutaDto g) -> g.estacion().precio())
                .thenComparingDouble(GasolineraEnRutaDto::kmDesdeOrigen));
        return encontradas;
    }

    /**
     * El punto de la ruta más cercano a la gasolinera.
     *
     * @return {@code [distancia a la ruta, km desde el origen]}, o null si ningún tramo
     *         queda a menos de {@code desvioKm}
     */
    static double[] masCercano(double lat, double lon, List<double[]> puntos, double[] acumulado, double desvioKm) {
        double kx = KM_POR_GRADO * Math.cos(Math.toRadians(lat));
        double mejor = Double.MAX_VALUE;
        double kmEnRuta = 0;
        for (int i = 0; i < puntos.size() - 1; i++) {
            double ax = (puntos.get(i)[1] - lon) * kx;
            double ay = (puntos.get(i)[0] - lat) * KM_POR_GRADO;
            double bx = (puntos.get(i + 1)[1] - lon) * kx;
            double by = (puntos.get(i + 1)[0] - lat) * KM_POR_GRADO;
            // Descarte rápido: el tramo entero queda lejos por un lado
            if ((ax > desvioKm && bx > desvioKm) || (ax < -desvioKm && bx < -desvioKm)
                    || (ay > desvioKm && by > desvioKm) || (ay < -desvioKm && by < -desvioKm)) {
                continue;
            }
            double dx = bx - ax;
            double dy = by - ay;
            double largo2 = dx * dx + dy * dy;
            double t = largo2 == 0 ? 0 : Math.max(0, Math.min(1, -(ax * dx + ay * dy) / largo2));
            double px = ax + t * dx;
            double py = ay + t * dy;
            double distancia = Math.sqrt(px * px + py * py);
            if (distancia < mejor) {
                mejor = distancia;
                kmEnRuta = acumulado[i] + t * (acumulado[i + 1] - acumulado[i]);
            }
        }
        return mejor <= desvioKm ? new double[]{mejor, kmEnRuta} : null;
    }

    private static double[] acumulados(List<double[]> puntos) {
        double[] km = new double[puntos.size()];
        for (int i = 1; i < puntos.size(); i++) {
            double[] a = puntos.get(i - 1);
            double[] b = puntos.get(i);
            km[i] = km[i - 1] + Geo.distanciaKm(a[0], a[1], b[0], b[1]);
        }
        return km;
    }

    /** {@code [latMin, latMax, lonMin, lonMax]} de la ruta, ensanchada con el desvío. */
    private static double[] caja(List<double[]> puntos, double desvioKm) {
        double latMin = 90;
        double latMax = -90;
        double lonMin = 180;
        double lonMax = -180;
        for (double[] p : puntos) {
            latMin = Math.min(latMin, p[0]);
            latMax = Math.max(latMax, p[0]);
            lonMin = Math.min(lonMin, p[1]);
            lonMax = Math.max(lonMax, p[1]);
        }
        double margenLat = desvioKm / KM_POR_GRADO;
        // Con la latitud más alejada del ecuador: el margen más ancho que pueda hacer falta
        double coseno = Math.cos(Math.toRadians(Math.max(Math.abs(latMin), Math.abs(latMax))));
        double margenLon = desvioKm / (KM_POR_GRADO * coseno);
        return new double[]{latMin - margenLat, latMax + margenLat, lonMin - margenLon, lonMax + margenLon};
    }

    static List<double[]> paraPintar(List<double[]> puntos) {
        if (puntos.size() <= PUNTOS_PARA_PINTAR) {
            return puntos;
        }
        int paso = (int) Math.ceil(puntos.size() / (double) PUNTOS_PARA_PINTAR);
        List<double[]> pocos = new ArrayList<>();
        for (int i = 0; i < puntos.size(); i += paso) {
            pocos.add(puntos.get(i));
        }
        pocos.add(puntos.get(puntos.size() - 1));
        return pocos;
    }

    private static BigDecimal media(List<BigDecimal> precios) {
        if (precios.isEmpty()) {
            return null;
        }
        return precios.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(precios.size()), 3, RoundingMode.HALF_UP);
    }

    private static double redondear(double valor, int decimales) {
        return BigDecimal.valueOf(valor).setScale(decimales, RoundingMode.HALF_UP).doubleValue();
    }
}
