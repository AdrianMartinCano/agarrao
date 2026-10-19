package dev.pimon.listillo.gasolina.repostajes;

import dev.pimon.common.exception.AppException;
import dev.pimon.listillo.gasolina.estaciones.Carburante;
import dev.pimon.listillo.gasolina.estaciones.CatalogoEstaciones;
import dev.pimon.listillo.gasolina.estaciones.Estacion;
import dev.pimon.listillo.gasolina.repostajes.dto.RepostajesDtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Vehículos y repostajes.
 *
 * Buena parte de lo que se prueba aquí es de PROPIEDAD: que nadie pueda mirar ni tocar
 * los vehículos y repostajes de otra persona pasando un id ajeno. Es lo que separa un
 * historial privado de una base de datos pública.
 */
class RepostajesServiceTest {

    /** En Agarrao los coches son del hogar: «usuario» del test original = hogar. */
    private static final java.util.UUID USUARIO = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final java.util.UUID OTRO = java.util.UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String VEHICULO = "vehiculo-1";

    private VehiculoRepository  vehiculos;
    private RepostajeRepository repostajes;
    private CatalogoEstaciones  catalogo;
    private RepostajesService   servicio;

    private List<Repostaje> guardados;

    @BeforeEach
    void setUp() {
        vehiculos  = mock(VehiculoRepository.class);
        repostajes = mock(RepostajeRepository.class);
        catalogo   = mock(CatalogoEstaciones.class);
        guardados  = new ArrayList<>();

        when(repostajes.save(any())).thenAnswer(i -> {
            Repostaje r = i.getArgument(0);
            guardados.add(r);
            return r;
        });
        when(vehiculos.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repostajes.findByVehiculoIdOrderByFechaAsc(anyString())).thenReturn(List.of());

        servicio = new RepostajesService(vehiculos, repostajes, catalogo);
    }

    // ── Propiedad: lo que impide leer o tocar lo de otro ── //

    @Test
    @DisplayName("No se puede apuntar un repostaje en el vehículo de otro")
    void repostajeEnVehiculoAjeno() {
        // El repositorio busca por id Y usuario a la vez: el coche de otro no aparece.
        when(vehiculos.findByIdAndHogarId(VEHICULO, OTRO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(OTRO, "yo", peticionRepostaje()))
                .isInstanceOf(AppException.class);

        verify(repostajes, never()).save(any());
    }

    @Test
    @DisplayName("Pedir las estadísticas del vehículo de otro no las devuelve")
    void estadisticasDeVehiculoAjeno() {
        when(vehiculos.findByIdAndHogarId(VEHICULO, OTRO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.estadisticas(OTRO, VEHICULO))
                .isInstanceOf(AppException.class)
                // 404 y no 403: un "prohibido" confirmaría que ese vehículo existe.
                .hasMessageContaining("no existe");
    }

    @Test
    @DisplayName("No se puede editar el vehículo de otro")
    void editarVehiculoAjeno() {
        when(vehiculos.findByIdAndHogarId(VEHICULO, OTRO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizarVehiculo(OTRO, VEHICULO,
                new ActualizarVehiculoRequest("Robado", Carburante.GLP, null)))
                .isInstanceOf(AppException.class);

        verify(vehiculos, never()).save(any());
    }

    @Test
    @DisplayName("No se puede borrar el repostaje de otro")
    void borrarRepostajeAjeno() {
        when(repostajes.findByIdAndHogarId("repostaje-1", OTRO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminar(OTRO, "repostaje-1"))
                .isInstanceOf(AppException.class);

        verify(repostajes, never()).delete(any());
    }

    // ── Vehículos ── //

    @Test
    @DisplayName("Hay un tope de vehículos por usuario")
    void topeDeVehiculos() {
        when(vehiculos.countByHogarIdAndActivoTrue(USUARIO)).thenReturn(20L);

        assertThatThrownBy(() -> servicio.crearVehiculo(USUARIO,
                new CrearVehiculoRequest("El vigésimo primero", Carburante.GASOLINA_95_E5, null)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("máximo");
    }

    @Test
    @DisplayName("La matrícula se guarda en mayúsculas y sin espacios")
    void matriculaNormalizada() {
        when(vehiculos.countByHogarIdAndActivoTrue(USUARIO)).thenReturn(0L);

        VehiculoDto creado = servicio.crearVehiculo(USUARIO,
                new CrearVehiculoRequest("El Golf", Carburante.GASOLINA_95_E5, "  1234 bcd  "));

        // Así no depende de cómo lo teclee cada uno.
        assertThat(creado.matricula()).isEqualTo("1234 BCD");
    }

    @Test
    @DisplayName("Una matrícula vacía se guarda como null, no como cadena vacía")
    void matriculaVacia() {
        when(vehiculos.countByHogarIdAndActivoTrue(USUARIO)).thenReturn(0L);

        VehiculoDto creado = servicio.crearVehiculo(USUARIO,
                new CrearVehiculoRequest("La moto", Carburante.GASOLINA_95_E5, "   "));

        assertThat(creado.matricula()).isNull();
    }

    @Test
    @DisplayName("Archivar un vehículo no lo borra: su historial se conserva")
    void archivarNoBorra() {
        Vehiculo vehiculo = vehiculo();
        when(vehiculos.findByIdAndHogarId(VEHICULO, USUARIO)).thenReturn(Optional.of(vehiculo));

        servicio.archivarVehiculo(USUARIO, VEHICULO);

        assertThat(vehiculo.isActivo()).isFalse();
        // Lo que se querría conservar de un coche vendido es justo su historial.
        verify(vehiculos, never()).delete(any());
    }

    // ── Repostajes ── //

    @Test
    @DisplayName("El importe se calcula, no se pide")
    void importeCalculado() {
        prepararVehiculo();

        servicio.crear(USUARIO, "yo", peticionRepostaje());

        // 40 L × 1,699 = 67,96. Pedir los tres números invita a que no cuadren.
        assertThat(guardados).singleElement()
                .extracting(Repostaje::getImporte)
                .isEqualTo(new BigDecimal("67.96"));
    }

    @Test
    @DisplayName("Sin decir nada, se da por hecho que se llenó el depósito")
    void depositoLlenoPorDefecto() {
        prepararVehiculo();

        servicio.crear(USUARIO, "yo", new CrearRepostajeRequest(
                VEHICULO, LocalDate.now(), Carburante.GASOLINA_95_E5,
                new BigDecimal("40"), new BigDecimal("1.699"), null,
                null, null, null, null));

        // Es lo que hace la mayoría de la gente.
        assertThat(guardados).singleElement().matches(Repostaje::isDepositoLleno);
    }

    @Test
    @DisplayName("Al elegir gasolinera se congelan sus datos y se calcula la media provincial")
    void congelaEstacionYCalculaMedia() {
        prepararVehiculo();
        when(catalogo.porId("4982")).thenReturn(Optional.of(
                estacion("4982", "CEPSA", "Madrid", "MADRID", "1.700")));
        when(catalogo.todas()).thenReturn(List.of(
                estacion("4982", "CEPSA", "Madrid", "MADRID", "1.700"),
                estacion("2", "REPSOL", "Madrid", "MADRID", "1.800"),
                // De otra provincia: no debe entrar en la media de Madrid.
                estacion("3", "BP", "Sevilla", "SEVILLA", "1.500")));

        servicio.crear(USUARIO, "yo", peticionConEstacion("4982"));

        Repostaje guardado = guardados.get(0);
        // Los datos de la gasolinera se copian: si cierra, el historial no se queda cojo.
        assertThat(guardado.getEstacionNombre()).isEqualTo("CEPSA");
        assertThat(guardado.getEstacionMunicipio()).isEqualTo("Madrid");
        assertThat(guardado.getEstacionDireccion()).isEqualTo("Calle de prueba");
        // Media de Madrid: (1,700 + 1,800) / 2 = 1,750. Sevilla no cuenta.
        assertThat(guardado.getPrecioMedioProvincia()).isEqualByComparingTo("1.750");
    }

    @Test
    @DisplayName("Si la gasolinera ya no está en el catálogo, el repostaje se guarda igual")
    void estacionDesconocida() {
        prepararVehiculo();
        when(catalogo.porId("no-existe")).thenReturn(Optional.empty());

        servicio.crear(USUARIO, "yo", peticionConEstacion("no-existe"));

        // Se guarda sin datos de estación en vez de rechazarlo: el repostaje ocurrió.
        assertThat(guardados).singleElement()
                .extracting(Repostaje::getEstacionNombre)
                .isNull();
    }

    @Test
    @DisplayName("Sin gasolinera no hay media provincial que comparar")
    void sinEstacionNoHayMedia() {
        prepararVehiculo();

        servicio.crear(USUARIO, "yo", peticionRepostaje());

        assertThat(guardados).singleElement()
                .extracting(Repostaje::getPrecioMedioProvincia)
                .isNull();
    }

    // ── La zona ── //

    @Test
    @DisplayName("La zona solo mira lo que había a 10 km, no toda la provincia")
    void zonaIgnoraLoQueEstaLejos() {
        prepararVehiculo();
        Estacion elegida = estacion("4982", "CEPSA", "Madrid", "MADRID", "1.700");
        when(catalogo.porId("4982")).thenReturn(Optional.of(elegida));
        when(catalogo.todas()).thenReturn(List.of(
                elegida,
                estacion("2", "REPSOL", "Madrid", "MADRID", "1.800", 4),
                estacion("3", "BP", "Madrid", "MADRID", "1.600", 9),
                // Misma provincia, pero a 60 km: nunca fue una alternativa y no debe
                // arrastrar la media. Es justo lo que hacía inútil la comparación anterior.
                estacion("4", "LOW COST", "Aranjuez", "MADRID", "1.100", 60)));

        servicio.crear(USUARIO, "yo", peticionConEstacion("4982"));

        Repostaje guardado = guardados.get(0);
        // (1,700 + 1,800 + 1,600) / 3 = 1,700. La de 1,100 se queda fuera.
        assertThat(guardado.getPrecioMedioZona()).isEqualByComparingTo("1.700");
        assertThat(guardado.getPrecioMinimoZona()).isEqualByComparingTo("1.600");
        assertThat(guardado.getRadioZonaKm()).isEqualTo(10);
        // La provincial se sigue guardando, pero ya no es la que se enseña.
        assertThat(guardado.getPrecioMedioProvincia()).isEqualByComparingTo("1.550");
    }

    @Test
    @DisplayName("La gasolinera elegida cuenta dentro de su propia zona")
    void laElegidaEntraEnSuZona() {
        prepararVehiculo();
        // Se reposta a 1,699 en la más barata de las tres.
        Estacion elegida = estacion("4982", "CEPSA", "Madrid", "MADRID", "1.699");
        when(catalogo.porId("4982")).thenReturn(Optional.of(elegida));
        when(catalogo.todas()).thenReturn(List.of(
                elegida,
                estacion("2", "REPSOL", "Madrid", "MADRID", "1.800", 3),
                estacion("3", "BP", "Madrid", "MADRID", "1.900", 6)));

        RepostajeDto dto = servicio.crear(USUARIO, "yo", peticionConEstacion("4982"));

        // Si la elegida no entrara en el conjunto, "la más barata de la zona" jamás podría
        // ser donde repostaste y el mensaje nunca sería felicitación.
        assertThat(guardados.get(0).getPrecioMinimoZona()).isEqualByComparingTo("1.699");
        assertThat(dto.dejadoEnLaMesa()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Donde hay poco donde elegir, el radio se ensancha a 25 km")
    void zonaSeAmpliaEnZonaRural() {
        prepararVehiculo();
        Estacion elegida = estacion("4982", "CEPSA", "Soria", "SORIA", "1.700");
        when(catalogo.porId("4982")).thenReturn(Optional.of(elegida));
        when(catalogo.todas()).thenReturn(List.of(
                elegida,
                // Solo dos a 10 km: no llegan al mínimo, así que se prueba con 25.
                estacion("2", "REPSOL", "Soria", "SORIA", "1.800", 7),
                estacion("3", "BP", "Almazán", "SORIA", "1.500", 20)));

        servicio.crear(USUARIO, "yo", peticionConEstacion("4982"));

        Repostaje guardado = guardados.get(0);
        assertThat(guardado.getRadioZonaKm()).isEqualTo(25);
        // (1,700 + 1,800 + 1,500) / 3 = 1,666...
        assertThat(guardado.getPrecioMedioZona()).isEqualByComparingTo("1.667");
        assertThat(guardado.getPrecioMinimoZona()).isEqualByComparingTo("1.500");
    }

    @Test
    @DisplayName("Si ni ampliando hay con qué comparar, no se inventa una zona")
    void sinVecinosNoHayZona() {
        prepararVehiculo();
        Estacion elegida = estacion("4982", "CEPSA", "Teruel", "TERUEL", "1.700");
        when(catalogo.porId("4982")).thenReturn(Optional.of(elegida));
        when(catalogo.todas()).thenReturn(List.of(
                elegida,
                estacion("2", "REPSOL", "Teruel", "TERUEL", "1.800", 40)));

        RepostajeDto dto = servicio.crear(USUARIO, "yo", peticionConEstacion("4982"));

        // Ser "la más barata" de un conjunto de uno no dice nada, así que mejor callarse:
        // el repostaje se queda con la provincial, como los antiguos.
        assertThat(guardados.get(0).getPrecioMedioZona()).isNull();
        assertThat(guardados.get(0).getRadioZonaKm()).isNull();
        assertThat(dto.dejadoEnLaMesa()).isNull();
        assertThat(dto.diferenciaMedia()).isNotNull();
    }

    @Test
    @DisplayName("Lo dejado en la mesa nunca es negativo")
    void dejadoEnLaMesaNuncaEsNegativo() {
        prepararVehiculo();
        // El ticket dice 1,699 pero el catálogo ya tiene 1,750: la gasolinera subió el
        // precio ese mismo día. Sin el tope en cero saldría un "ahorro" inventado.
        Estacion elegida = estacion("4982", "CEPSA", "Madrid", "MADRID", "1.750");
        when(catalogo.porId("4982")).thenReturn(Optional.of(elegida));
        when(catalogo.todas()).thenReturn(List.of(
                elegida,
                estacion("2", "REPSOL", "Madrid", "MADRID", "1.800", 3),
                estacion("3", "BP", "Madrid", "MADRID", "1.900", 6)));

        RepostajeDto dto = servicio.crear(USUARIO, "yo", peticionConEstacion("4982"));

        assertThat(dto.dejadoEnLaMesa()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Un repostaje antiguo sin zona se sigue comparando con la provincia")
    void historicoSinZonaUsaLaProvincia() {
        // 40 litros a 1,650 con la provincia a 1,750: (1,750 − 1,650) × 40 = 4,00 €.
        Repostaje antiguo = repostaje("2026-03-01", "40", "1.650", "1.750");
        prepararVehiculo();
        when(repostajes.findByVehiculoIdOrderByFechaAsc(VEHICULO)).thenReturn(List.of(antiguo));

        EstadisticasDto estadisticas = servicio.estadisticas(USUARIO, VEHICULO);

        assertThat(estadisticas.ahorroFrenteALaMedia()).isEqualByComparingTo("4.00");
        // Sin zona no se puede saber qué se dejó de ahorrar: no se inventa un cero.
        assertThat(estadisticas.dejadoEnLaMesa()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Un repostaje de antes de guardar la dirección la toma del catálogo")
    void direccionDeLosAntiguos() {
        Repostaje antiguo = repostaje("2026-03-01", "40", "1.650", "1.750");
        antiguo.setEstacionId("4982");
        when(repostajes.findByHogarIdOrderByFechaDesc(USUARIO)).thenReturn(List.of(antiguo));
        when(catalogo.porId("4982")).thenReturn(Optional.of(
                estacion("4982", "CEPSA", "Madrid", "MADRID", "1.700")));

        assertThat(servicio.listar(USUARIO)).singleElement()
                .extracting(RepostajeDto::estacionDireccion)
                .isEqualTo("Calle de prueba");
    }

    // ── Editar ── //

    @Test
    @DisplayName("Al corregir los kilómetros NO se toca el precio medio de la provincia")
    void editarNoRecalculaLaMediaSiNoCambiaLaEstacion() {
        Repostaje existente = repostaje("2026-03-01", "40", "1.650", "1.750");
        existente.setEstacionId("4982");
        existente.setEstacionNombre("CEPSA");
        prepararVehiculo();
        when(repostajes.findByIdAndHogarId("r1", USUARIO)).thenReturn(Optional.of(existente));

        // Solo se corrige una errata en el cuentakilómetros.
        servicio.actualizar(USUARIO, "r1", new ActualizarRepostajeRequest(
                VEHICULO, LocalDate.parse("2026-03-01"), Carburante.GASOLINA_95_E5,
                new BigDecimal("40"), new BigDecimal("1.650"), null, 172_450, true, "4982", null));

        // La media es un dato CON FECHA: la del día en que se repostó. Recalcularla
        // metería el precio de hoy en un repostaje de hace un mes.
        assertThat(existente.getPrecioMedioProvincia()).isEqualByComparingTo("1.750");
        assertThat(existente.getKilometros()).isEqualTo(172_450);
    }

    @Test
    @DisplayName("Si se cambia la gasolinera, sus datos y la media se rehacen")
    void editarCambiandoEstacion() {
        Repostaje existente = repostaje("2026-03-01", "40", "1.650", "1.750");
        existente.setEstacionId("4982");
        existente.setEstacionNombre("CEPSA");
        prepararVehiculo();
        when(repostajes.findByIdAndHogarId("r1", USUARIO)).thenReturn(Optional.of(existente));
        when(catalogo.porId("otra")).thenReturn(Optional.of(
                estacion("otra", "REPSOL", "Sevilla", "SEVILLA", "1.900")));
        when(catalogo.todas()).thenReturn(List.of(
                estacion("otra", "REPSOL", "Sevilla", "SEVILLA", "1.900")));

        servicio.actualizar(USUARIO, "r1", new ActualizarRepostajeRequest(
                VEHICULO, LocalDate.parse("2026-03-01"), Carburante.GASOLINA_95_E5,
                new BigDecimal("40"), new BigDecimal("1.650"), null, null, true, "otra", null));

        assertThat(existente.getEstacionNombre()).isEqualTo("REPSOL");
        // Del sitio nuevo no sabemos qué valía aquel día: se pone el de hoy, que al menos
        // es de la provincia correcta.
        assertThat(existente.getPrecioMedioProvincia()).isEqualByComparingTo("1.900");
    }

    @Test
    @DisplayName("Quitar la gasolinera limpia también su media")
    void editarQuitandoEstacion() {
        Repostaje existente = repostaje("2026-03-01", "40", "1.650", "1.750");
        existente.setEstacionId("4982");
        existente.setEstacionNombre("CEPSA");
        prepararVehiculo();
        when(repostajes.findByIdAndHogarId("r1", USUARIO)).thenReturn(Optional.of(existente));

        servicio.actualizar(USUARIO, "r1", new ActualizarRepostajeRequest(
                VEHICULO, LocalDate.parse("2026-03-01"), Carburante.GASOLINA_95_E5,
                new BigDecimal("40"), new BigDecimal("1.650"), null, null, true, null, null));

        // Dejar el nombre viejo con la media vieja sería peor que no tener ninguno.
        assertThat(existente.getEstacionNombre()).isNull();
        assertThat(existente.getPrecioMedioProvincia()).isNull();
    }

    @Test
    @DisplayName("El importe se recalcula al cambiar litros o precio")
    void editarRecalculaImporte() {
        Repostaje existente = repostaje("2026-03-01", "40", "1.650", null);
        prepararVehiculo();
        when(repostajes.findByIdAndHogarId("r1", USUARIO)).thenReturn(Optional.of(existente));

        servicio.actualizar(USUARIO, "r1", new ActualizarRepostajeRequest(
                VEHICULO, LocalDate.parse("2026-03-01"), Carburante.GASOLINA_95_E5,
                new BigDecimal("30"), new BigDecimal("1.800"), null, null, true, null, null));

        assertThat(existente.getImporte()).isEqualByComparingTo("54.00");
    }

    @Test
    @DisplayName("No se puede mover un repostaje al vehículo de otro")
    void editarMoviendoloAVehiculoAjeno() {
        Repostaje existente = repostaje("2026-03-01", "40", "1.650", null);
        when(repostajes.findByIdAndHogarId("r1", USUARIO)).thenReturn(Optional.of(existente));
        // El vehículo de destino no es suyo.
        when(vehiculos.findByIdAndHogarId("vehiculo-ajeno", USUARIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizar(USUARIO, "r1", new ActualizarRepostajeRequest(
                "vehiculo-ajeno", LocalDate.parse("2026-03-01"), Carburante.GASOLINA_95_E5,
                new BigDecimal("40"), new BigDecimal("1.650"), null, null, true, null, null)))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("No se puede editar el repostaje de otro")
    void editarRepostajeAjeno() {
        when(repostajes.findByIdAndHogarId("r1", OTRO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizar(OTRO, "r1", new ActualizarRepostajeRequest(
                VEHICULO, LocalDate.now(), Carburante.GASOLINA_95_E5,
                new BigDecimal("40"), new BigDecimal("1.650"), null, null, true, null, null)))
                .isInstanceOf(AppException.class);
    }

    // ── Estadísticas ── //

    @Test
    @DisplayName("El ahorro compara lo pagado con la media, litro a litro")
    void ahorroFrenteALaMedia() {
        Vehiculo vehiculo = vehiculo();
        when(vehiculos.findByIdAndHogarId(VEHICULO, USUARIO)).thenReturn(Optional.of(vehiculo));
        when(repostajes.findByVehiculoIdOrderByFechaAsc(VEHICULO)).thenReturn(List.of(
                // 40 L a 1,65 con la media en 1,75 → 4 € ahorrados
                repostaje("2026-03-01", "40", "1.650", "1.750"),
                // 30 L a 1,80 con la media en 1,75 → 1,50 € de más
                repostaje("2026-03-15", "30", "1.800", "1.750")));

        EstadisticasDto estadisticas = servicio.estadisticas(USUARIO, VEHICULO);

        assertThat(estadisticas.ahorroFrenteALaMedia()).isEqualByComparingTo("2.50");
    }

    @Test
    @DisplayName("Los repostajes sin media provincial no cuentan para el ahorro")
    void ahorroIgnoraLosSinMedia() {
        Vehiculo vehiculo = vehiculo();
        when(vehiculos.findByIdAndHogarId(VEHICULO, USUARIO)).thenReturn(Optional.of(vehiculo));
        when(repostajes.findByVehiculoIdOrderByFechaAsc(VEHICULO)).thenReturn(List.of(
                repostaje("2026-03-01", "40", "1.650", "1.750"),
                // Este se registró sin elegir gasolinera: no hay con qué compararlo.
                repostaje("2026-03-15", "30", "1.800", null)));

        assertThat(servicio.estadisticas(USUARIO, VEHICULO).ahorroFrenteALaMedia())
                .isEqualByComparingTo("4.00");
    }

    @Test
    @DisplayName("El gasto se agrupa por mes")
    void gastoMensual() {
        Vehiculo vehiculo = vehiculo();
        when(vehiculos.findByIdAndHogarId(VEHICULO, USUARIO)).thenReturn(Optional.of(vehiculo));
        when(repostajes.findByVehiculoIdOrderByFechaAsc(VEHICULO)).thenReturn(List.of(
                repostaje("2026-03-01", "40", "1.650", null),
                repostaje("2026-03-20", "30", "1.700", null),
                repostaje("2026-04-05", "35", "1.700", null)));

        List<GastoMensualDto> meses = servicio.estadisticas(USUARIO, VEHICULO).gastoMensual();

        assertThat(meses).hasSize(2);
        assertThat(meses.get(0).mes()).isEqualTo("2026-03");
        // 40 × 1,65 + 30 × 1,70 = 66 + 51 = 117
        assertThat(meses.get(0).importe()).isEqualByComparingTo("117.00");
    }

    // ── apoyo ── //

    private void prepararVehiculo() {
        when(vehiculos.findByIdAndHogarId(VEHICULO, USUARIO)).thenReturn(Optional.of(vehiculo()));
    }

    private static Vehiculo vehiculo() {
        Vehiculo vehiculo = new Vehiculo(USUARIO, "El Golf", Carburante.GASOLINA_95_E5, null);
        vehiculo.setId(VEHICULO);
        return vehiculo;
    }

    private static CrearRepostajeRequest peticionRepostaje() {
        return new CrearRepostajeRequest(VEHICULO, LocalDate.now(), Carburante.GASOLINA_95_E5,
                new BigDecimal("40"), new BigDecimal("1.699"), null, 10_000, true, null, null);
    }

    @org.junit.jupiter.api.Test
    void elImporteDelTicketSeGuardaTalCualSiCuadra() {
        // 20 € a 1,689: 11,84 l. Recalculado daría 20,00 o 20,01 según el redondeo de los litros.
        assertThat(RepostajesService.importe(new BigDecimal("11.84"), new BigDecimal("1.689"), new BigDecimal("20")))
                .isEqualByComparingTo("20.00");
        assertThat(RepostajesService.importe(new BigDecimal("11.84"), new BigDecimal("1.689"), null))
                .isEqualByComparingTo("20.00");
        assertThatThrownBy(() -> RepostajesService.importe(new BigDecimal("40"), new BigDecimal("1.699"), new BigDecimal("20")))
                .isInstanceOf(AppException.class).hasMessageContaining("no cuadran");
    }

    private static CrearRepostajeRequest peticionConEstacion(String estacionId) {
        return new CrearRepostajeRequest(VEHICULO, LocalDate.now(), Carburante.GASOLINA_95_E5,
                new BigDecimal("40"), new BigDecimal("1.699"), null, 10_000, true, estacionId, null);
    }

    private static Repostaje repostaje(String fecha, String litros, String precio, String media) {
        Repostaje repostaje = new Repostaje();
        repostaje.setFecha(LocalDate.parse(fecha));
        repostaje.setLitros(new BigDecimal(litros));
        repostaje.setPrecioLitro(new BigDecimal(precio));
        repostaje.setImporte(new BigDecimal(litros).multiply(new BigDecimal(precio)));
        repostaje.setCarburante(Carburante.GASOLINA_95_E5);
        repostaje.setDepositoLleno(true);
        if (media != null) {
            repostaje.setPrecioMedioProvincia(new BigDecimal(media));
        }
        return repostaje;
    }

    private static Estacion estacion(String id, String rotulo, String municipio,
                                     String provincia, String precio) {
        return estacion(id, rotulo, municipio, provincia, precio, 0);
    }

    /**
     * La misma estación, desplazada {@code kmAlNorte} kilómetros.
     *
     * Un grado de latitud son ~111,32 km en cualquier punto de la Tierra, así que moverse
     * solo en latitud da distancias predecibles sin depender de la longitud, que sí se
     * encoge según subes hacia el polo.
     */
    private static Estacion estacion(String id, String rotulo, String municipio,
                                     String provincia, String precio, double kmAlNorte) {
        return new Estacion(id, rotulo, "Calle de prueba", municipio, municipio, provincia,
                "28001", "L-D: 24H", 40.4 + kmAlNorte / 111.32, -3.7,
                Map.of(Carburante.GASOLINA_95_E5, new BigDecimal(precio)));
    }
}
