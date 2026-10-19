package dev.pimon.listillo.producto;

import dev.pimon.common.exception.AppException;
import dev.pimon.listillo.producto.dto.NombrarProductoRequest;
import dev.pimon.listillo.producto.dto.ProductoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductoServiceTest {

    private static final String LECHE = "8480000107947";
    private static final DatosProducto DATOS_LECHE =
            new DatosProducto("Leche semidesnatada", "Hacendado", "1 L", null, "en:milks");

    private final Map<String, Producto> base = new HashMap<>();
    private final ProductoRepository repositorio = mock(ProductoRepository.class);
    private final CatalogoProductos catalogo = mock(CatalogoProductos.class);
    private final ProductoService servicio = new ProductoService(repositorio, catalogo);

    @BeforeEach
    void baseEnMemoria() {
        when(repositorio.findById(any())).thenAnswer(i -> Optional.ofNullable(base.get(i.<String>getArgument(0))));
        when(repositorio.save(any())).thenAnswer(i -> {
            Producto p = i.getArgument(0);
            base.put(p.getEan(), p);
            return p;
        });
    }

    @Test
    @DisplayName("la primera vez pregunta a OFF y la segunda tira de lo guardado")
    void cache() {
        when(catalogo.buscar(LECHE)).thenReturn(Optional.of(DATOS_LECHE));

        ProductoDto primera = servicio.buscar(LECHE);
        servicio.buscar(LECHE);

        assertThat(primera.nombre()).isEqualTo("Leche semidesnatada");
        assertThat(primera.fuente()).isEqualTo("Open Food Facts");
        verify(catalogo, times(1)).buscar(LECHE);
    }

    @Test
    @DisplayName("lo que OFF no conoce se recuerda y no se vuelve a preguntar enseguida")
    void desconocido() {
        when(catalogo.buscar(LECHE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.buscar(LECHE)).isInstanceOf(AppException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> servicio.buscar(LECHE)).isInstanceOf(AppException.class);

        verify(catalogo, times(1)).buscar(LECHE);
    }

    @Test
    @DisplayName("si OFF no responde y no hay nada guardado es un 503; un código malo no llega a OFF")
    void caido() {
        when(catalogo.buscar(LECHE)).thenThrow(new CatalogoProductos.CatalogoNoDisponible("caído", null));

        assertThatThrownBy(() -> servicio.buscar(LECHE)).extracting("status").isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThatThrownBy(() -> servicio.buscar("8480000107948")).extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
        verify(catalogo, never()).buscar("8480000107948");
    }

    @Test
    @DisplayName("nombrar un desconocido lo deja conocido para todos; lo de OFF no se pisa")
    void nombrar() {
        when(catalogo.buscar(LECHE)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.buscar(LECHE)).isInstanceOf(AppException.class);

        ProductoDto nombrado = servicio.nombrar(LECHE, new NombrarProductoRequest(" Leche del pueblo ", "", "1 L"), "u1");
        assertThat(nombrado.nombre()).isEqualTo("Leche del pueblo");
        assertThat(nombrado.marca()).isNull();
        assertThat(servicio.buscar(LECHE).origen()).isEqualTo(OrigenProducto.USUARIO);

        base.put("96385074", Producto.deOpenFoodFacts("96385074", DATOS_LECHE));
        assertThatThrownBy(() -> servicio.nombrar("96385074", new NombrarProductoRequest("Otra cosa", null, null), "u1"))
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);
    }
}
