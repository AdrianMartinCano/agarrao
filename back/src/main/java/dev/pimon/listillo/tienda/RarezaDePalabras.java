package dev.pimon.listillo.tienda;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cuánto dice una palabra en el nombre de un producto: «nutella» o «pepsi» salen en pocos
 * productos (dicen mucho: suelen ser la marca); «zero» o «azúcar», en muchos. Es el peso
 * (IDF, log(N / productos con esa palabra)) para comparar nombres entre súper.
 *
 * Se cuenta en memoria sobre todos los nombres (~30.000) y se rehace cada pocas horas: las
 * webs se importan una vez al día.
 */
@Component
public class RarezaDePalabras {

    private static final Duration VIGENCIA = Duration.ofHours(6);

    private final ProductoTiendaRepository productos;
    private volatile Map<String, Integer> enCuantos = Map.of();
    private volatile int total;
    private volatile Instant contadoEn = Instant.EPOCH;

    public RarezaDePalabras(ProductoTiendaRepository productos) {
        this.productos = productos;
    }

    /** El peso de una palabra ya normalizada y en singular. Las que no salen nunca, las que más. */
    public double peso(String palabra) {
        contarSiHaceFalta();
        int n = enCuantos.getOrDefault(palabra, 0);
        return Math.log((total + 1.0) / (n + 1.0)) + 1;
    }

    private void contarSiHaceFalta() {
        if (Instant.now().isBefore(contadoEn.plus(VIGENCIA)) && total > 0) {
            return;
        }
        synchronized (this) {
            if (Instant.now().isBefore(contadoEn.plus(VIGENCIA)) && total > 0) {
                return;
            }
            List<String> nombres = productos.nombresNormalizados();
            Map<String, Integer> cuenta = new HashMap<>();
            for (String nombre : nombres) {
                Set<String> suyas = new HashSet<>(BuscadorService.significativas(nombre));
                suyas.forEach(p -> cuenta.merge(p, 1, Integer::sum));
            }
            enCuantos = cuenta;
            total = nombres.size();
            contadoEn = Instant.now();
        }
    }
}
