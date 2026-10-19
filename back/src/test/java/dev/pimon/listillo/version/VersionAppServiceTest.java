package dev.pimon.listillo.version;

import dev.pimon.common.exception.AppException;
import dev.pimon.listillo.version.dto.ActualizarVersionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VersionAppServiceTest {

    @Mock
    private VersionAppRepository repositorio;

    private VersionAppService servicio;

    @BeforeEach
    void setUp() {
        servicio = new VersionAppService(repositorio);
    }

    @Test
    @DisplayName("entre la mínima y la última: aviso opcional")
    void entreMinimaYUltimaEsOpcional() throws Exception {
        when(repositorio.findById(VersionApp.ID)).thenReturn(Optional.of(publicada("1.2.0", "1.0.0")));

        assertThat(servicio.comprobar("1.1.5").estado()).isEqualTo(EstadoVersion.OPCIONAL);
    }

    @Test
    @DisplayName("una instalada más nueva que la publicada cuenta como al día")
    void masNuevaQueLaPublicadaEstaAlDia() throws Exception {
        when(repositorio.findById(VersionApp.ID)).thenReturn(Optional.of(publicada("1.2.0", "1.0.0")));

        assertThat(servicio.comprobar("1.3.0").estado()).isEqualTo(EstadoVersion.AL_DIA);
    }

    @Test
    @DisplayName("no deja poner una mínima mayor que la última: bloquearía a todo el mundo")
    void minimaMayorQueUltimaSeRechaza() {
        var peticion = new ActualizarVersionRequest("1.0.0", "1.1.0", null);

        assertThatThrownBy(() -> servicio.actualizar(peticion))
                .isInstanceOf(AppException.class);
        verify(repositorio, never()).saveAndFlush(any());
    }

    /** VersionApp no tiene constructor público: solo la crea la migración. */
    private static VersionApp publicada(String ultima, String minima) throws Exception {
        Constructor<VersionApp> constructor = VersionApp.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        VersionApp version = constructor.newInstance();
        version.actualizar(ultima, minima, null);
        return version;
    }
}
