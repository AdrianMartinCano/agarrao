package dev.pimon.listillo.gasolina.favoritos;

import dev.pimon.common.exception.AppException;
import dev.pimon.listillo.gasolina.estaciones.Carburante;
import dev.pimon.listillo.gasolina.estaciones.CatalogoEstaciones;
import dev.pimon.listillo.gasolina.estaciones.Estacion;
import dev.pimon.listillo.gasolina.estaciones.dto.EstacionCercanaDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FavoritosServiceTest {

    private static final String USUARIO = "usuario-1";

    private FavoritoRepository repositorio;
    private CatalogoEstaciones catalogo;
    private FavoritosService   servicio;

    @BeforeEach
    void setUp() {
        repositorio = mock(FavoritoRepository.class);
        catalogo    = mock(CatalogoEstaciones.class);
        servicio    = new FavoritosService(repositorio, catalogo);
    }

    @Test
    @DisplayName("Añadir dos veces la misma estación no duplica ni da error")
    void anadirEsIdempotente() {
        when(catalogo.porId("4982")).thenReturn(Optional.of(estacion("4982")));
        when(repositorio.existsByUserIdAndEstacionId(USUARIO, "4982")).thenReturn(true);

        servicio.anadir(USUARIO, "4982");

        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("No se puede marcar como favorita una estación que no existe")
    void anadirEstacionInexistente() {
        when(catalogo.porId("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.anadir(USUARIO, "no-existe"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("no-existe");

        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("Quitar algo que no estaba en favoritos da 404")
    void eliminarInexistente() {
        when(repositorio.findByUserIdAndEstacionId(USUARIO, "4982")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminar(USUARIO, "4982"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("Un favorito cuya gasolinera ya no está en el catálogo se omite del listado")
    void favoritoDeEstacionDesaparecida() {
        when(repositorio.findByUserIdOrderByCreatedAtDesc(USUARIO)).thenReturn(List.of(
                new Favorito(USUARIO, "sigue-abierta"),
                new Favorito(USUARIO, "ya-cerro")
        ));
        when(catalogo.porId("sigue-abierta")).thenReturn(Optional.of(estacion("sigue-abierta")));
        // Una gasolinera que cierra desaparece del listado del Ministerio; la fila se
        // queda en la base, pero no hay nada que enseñar de ella.
        when(catalogo.porId("ya-cerro")).thenReturn(Optional.empty());

        List<EstacionCercanaDto> favoritos = servicio.listar(USUARIO, null, null, null);

        assertThat(favoritos).extracting(EstacionCercanaDto::id).containsExactly("sigue-abierta");
    }

    @Test
    @DisplayName("Sin ubicación, los favoritos vienen sin distancia en vez de con un valor inventado")
    void listarSinUbicacion() {
        when(repositorio.findByUserIdOrderByCreatedAtDesc(USUARIO))
                .thenReturn(List.of(new Favorito(USUARIO, "4982")));
        when(catalogo.porId("4982")).thenReturn(Optional.of(estacion("4982")));

        List<EstacionCercanaDto> favoritos = servicio.listar(USUARIO, null, null, null);

        assertThat(favoritos).singleElement()
                .extracting(EstacionCercanaDto::distanciaKm)
                .isNull();
    }

    @Test
    @DisplayName("Con ubicación, cada favorito trae su distancia")
    void listarConUbicacion() {
        when(repositorio.findByUserIdOrderByCreatedAtDesc(USUARIO))
                .thenReturn(List.of(new Favorito(USUARIO, "4982")));
        when(catalogo.porId("4982")).thenReturn(Optional.of(estacion("4982")));

        List<EstacionCercanaDto> favoritos = servicio.listar(USUARIO, 40.4168, -3.7038, Carburante.GASOLINA_95_E5);

        assertThat(favoritos).singleElement()
                .extracting(EstacionCercanaDto::distanciaKm)
                .isNotNull();
    }

    private static Estacion estacion(String id) {
        return new Estacion(id, "CEPSA", "Calle de prueba", "Madrid", "MADRID", "MADRID",
                "28001", "L-D: 24H", 40.4170, -3.7040,
                Map.of(Carburante.GASOLINA_95_E5, new BigDecimal("1.779")));
    }
}
