package dev.pimon.listillo.gasolina.repostajes;

import dev.pimon.common.exception.AppException;
import dev.pimon.listillo.gasolina.estaciones.Carburante;
import dev.pimon.listillo.gasolina.estaciones.CatalogoEstaciones;
import dev.pimon.listillo.gasolina.estaciones.Estacion;
import dev.pimon.listillo.gasolina.estaciones.Geo;
import dev.pimon.listillo.gasolina.repostajes.dto.RepostajesDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Vehículos, repostajes y las cuentas que salen de ellos. */
@Service
public class RepostajesService {

    /** Tope de vehículos por hogar: nadie tiene treinta coches, pero sí puede haber bots. */
    private static final int MAX_VEHICULOS = 20;

    /**
     * Radio en el que se mira "lo que había cerca" al comparar un repostaje.
     *
     * Diez kilómetros es lo que alguien se plantea desviarse por ahorrar unos céntimos.
     * Más lejos deja de ser una alternativa y vuelve a ser el problema de la media
     * provincial: comparar con gasolineras a las que nunca habrías ido.
     */
    private static final int RADIO_ZONA_KM = 10;

    /** Para zonas con poco donde elegir. Ver {@link #zona}. */
    private static final int RADIO_ZONA_AMPLIADO_KM = 25;

    /** Menos de esto no es una media: es el precio de una o dos gasolineras sueltas. */
    private static final int MINIMO_ESTACIONES_ZONA = 3;

    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("yyyy-MM");

    private final VehiculoRepository  vehiculos;
    private final RepostajeRepository repostajes;
    private final CatalogoEstaciones  catalogo;

    public RepostajesService(VehiculoRepository vehiculos, RepostajeRepository repostajes,
                             CatalogoEstaciones catalogo) {
        this.vehiculos  = vehiculos;
        this.repostajes = repostajes;
        this.catalogo   = catalogo;
    }

    // ── Vehículos ── //

    @Transactional(readOnly = true)
    public List<VehiculoDto> listarVehiculos(java.util.UUID hogarId) {
        return vehiculos.findByHogarIdAndActivoTrueOrderByCreatedAtAsc(hogarId).stream()
                .map(this::aDto)
                .toList();
    }

    @Transactional
    public VehiculoDto crearVehiculo(java.util.UUID hogarId, CrearVehiculoRequest peticion) {
        if (vehiculos.countByHogarIdAndActivoTrue(hogarId) >= MAX_VEHICULOS) {
            throw AppException.badRequest("Has llegado al máximo de " + MAX_VEHICULOS + " vehículos");
        }

        Vehiculo vehiculo = new Vehiculo(
                hogarId,
                peticion.nombre().trim(),
                peticion.carburante(),
                peticion.matricula() == null || peticion.matricula().isBlank()
                        ? null
                        : peticion.matricula().trim().toUpperCase());

        return aDto(vehiculos.save(vehiculo));
    }

    /**
     * Corrige los datos de un vehículo.
     *
     * Cambiar el carburante no toca el historial: cada repostaje guarda el suyo. Si
     * alguien se equivocó al dar de alta el coche, los repostajes que ya apuntó sí
     * llevaban el carburante correcto, y reescribirlos los estropearía.
     */
    @Transactional
    public VehiculoDto actualizarVehiculo(java.util.UUID hogarId, String vehiculoId,
                                          ActualizarVehiculoRequest peticion) {
        Vehiculo vehiculo = exigirVehiculo(hogarId, vehiculoId);

        vehiculo.setNombre(peticion.nombre().trim());
        vehiculo.setCarburante(peticion.carburante());
        vehiculo.setMatricula(peticion.matricula() == null || peticion.matricula().isBlank()
                ? null
                : peticion.matricula().trim().toUpperCase());

        return aDto(vehiculos.save(vehiculo));
    }

    /**
     * Archiva el vehículo en lugar de borrarlo.
     *
     * Un coche vendido deja de interesar, pero su historial no: cuánto gastaba y cuánto
     * consumía sigue siendo información. Borrarlo se lo llevaría todo por delante.
     */
    @Transactional
    public void archivarVehiculo(java.util.UUID hogarId, String vehiculoId) {
        Vehiculo vehiculo = exigirVehiculo(hogarId, vehiculoId);
        vehiculo.setActivo(false);
        vehiculos.save(vehiculo);
    }

    // ── Repostajes ── //

    @Transactional(readOnly = true)
    public List<RepostajeDto> listar(java.util.UUID hogarId) {
        Map<String, String> nombres = nombresDeVehiculos(hogarId);

        return repostajes.findByHogarIdOrderByFechaDesc(hogarId).stream()
                .map(r -> aDto(r, nombres.get(r.getVehiculoId())))
                .toList();
    }

    @Transactional
    public RepostajeDto crear(java.util.UUID hogarId, String usuarioId, CrearRepostajeRequest peticion) {
        Vehiculo vehiculo = exigirVehiculo(hogarId, peticion.vehiculoId());

        Repostaje repostaje = new Repostaje();
        repostaje.setHogarId(hogarId);
        repostaje.setCreadoPor(usuarioId);
        repostaje.setVehiculoId(vehiculo.getId());
        repostaje.setFecha(peticion.fecha());
        repostaje.setCarburante(peticion.carburante());
        repostaje.setLitros(peticion.litros());
        repostaje.setPrecioLitro(peticion.precioLitro());
        repostaje.setImporte(importe(peticion.litros(), peticion.precioLitro(), peticion.importe()));
        repostaje.setKilometros(peticion.kilometros());
        repostaje.setDepositoLleno(peticion.depositoLleno() == null || peticion.depositoLleno());
        repostaje.setNotas(peticion.notas());

        aplicarEstacion(repostaje, peticion.estacionId(), peticion.carburante());

        return aDto(repostajes.save(repostaje), vehiculo.getNombre());
    }

    /**
     * Corrige un repostaje ya apuntado.
     *
     * EL PRECIO MEDIO DE LA PROVINCIA SOLO SE RECALCULA SI CAMBIA LA GASOLINERA. Es un
     * dato con fecha: el del día en que se repostó. Recalcularlo al corregir unos
     * kilómetros metería el precio de HOY en un repostaje de hace un mes y estropearía
     * la comparación. Cuando sí cambia la gasolinera no hay alternativa —del sitio nuevo
     * no sabemos qué valía aquel día— y se pone el de hoy, que al menos es del sitio
     * correcto.
     */
    @Transactional
    public RepostajeDto actualizar(java.util.UUID hogarId, String repostajeId,
                                   ActualizarRepostajeRequest peticion) {
        Repostaje repostaje = repostajes.findByIdAndHogarId(repostajeId, hogarId)
                .orElseThrow(() -> AppException.notFound("Ese repostaje no existe"));

        // Se comprueba también el vehículo: mover un repostaje al coche de otro sería
        // colarle datos en su historial.
        Vehiculo vehiculo = exigirVehiculo(hogarId, peticion.vehiculoId());

        boolean cambiaEstacion = !java.util.Objects.equals(repostaje.getEstacionId(), peticion.estacionId());

        repostaje.setVehiculoId(vehiculo.getId());
        repostaje.setFecha(peticion.fecha());
        repostaje.setCarburante(peticion.carburante());
        repostaje.setLitros(peticion.litros());
        repostaje.setPrecioLitro(peticion.precioLitro());
        repostaje.setImporte(importe(peticion.litros(), peticion.precioLitro(), peticion.importe()));
        repostaje.setKilometros(peticion.kilometros());
        repostaje.setDepositoLleno(peticion.depositoLleno() == null || peticion.depositoLleno());
        repostaje.setNotas(peticion.notas());

        if (cambiaEstacion) {
            aplicarEstacion(repostaje, peticion.estacionId(), peticion.carburante());
        }

        return aDto(repostajes.save(repostaje), vehiculo.getNombre());
    }

    @Transactional
    public void eliminar(java.util.UUID hogarId, String repostajeId) {
        Repostaje repostaje = repostajes.findByIdAndHogarId(repostajeId, hogarId)
                .orElseThrow(() -> AppException.notFound("Ese repostaje no existe"));
        repostajes.delete(repostaje);
    }

    // ── Estadísticas ── //

    @Transactional(readOnly = true)
    public EstadisticasDto estadisticas(java.util.UUID hogarId, String vehiculoId) {
        exigirVehiculo(hogarId, vehiculoId);
        List<Repostaje> historial = repostajes.findByVehiculoIdOrderByFechaAsc(vehiculoId);

        List<CalculadoraConsumo.Tramo> tramos = CalculadoraConsumo.tramos(historial);

        return new EstadisticasDto(
                CalculadoraConsumo.consumoMedio(historial),
                suma(historial, Repostaje::getImporte),
                suma(historial, Repostaje::getLitros),
                tramos.stream().mapToInt(CalculadoraConsumo.Tramo::kilometros).sum(),
                ahorroFrenteALaMedia(historial),
                dejadoEnLaMesa(historial),
                tramos.stream()
                        .map(t -> new TramoConsumoDto(t.hasta(), t.kilometros(), t.litros(), t.consumo()))
                        .toList(),
                gastoMensual(historial),
                historial.stream()
                        .map(r -> new PrecioPagadoDto(r.getFecha(), r.getPrecioLitro(),
                                r.getPrecioMedioZona() != null
                                        ? r.getPrecioMedioZona()
                                        : r.getPrecioMedioProvincia()))
                        .toList());
    }

    // ── privado ── //

    /**
     * Lo que se pagó. Si llega el del ticket, ese: quien echa 20 € ha pagado 20,00 €, y
     * calcularlo como litros × precio lo desvía un céntimo por el redondeo de los litros
     * (el surtidor también redondea). Pero tiene que cuadrar con litros × precio: si se
     * aparta más de un 2 % (o de 10 céntimos, en repostajes pequeños), es una errata en
     * alguno de los tres y se avisa en vez de guardar números que no casan.
     * Sin importe, se calcula.
     */
    static BigDecimal importe(BigDecimal litros, BigDecimal precioLitro, BigDecimal delTicket) {
        BigDecimal calculado = litros.multiply(precioLitro).setScale(2, RoundingMode.HALF_UP);
        if (delTicket == null) {
            return calculado;
        }
        BigDecimal importe = delTicket.setScale(2, RoundingMode.HALF_UP);
        BigDecimal margen = calculado.multiply(new BigDecimal("0.02")).max(new BigDecimal("0.10"));
        if (importe.subtract(calculado).abs().compareTo(margen) > 0) {
            throw AppException.badRequest("Los litros, el precio y el importe no cuadran: "
                    + litros + " l × " + precioLitro + " €/l son " + calculado + " €, no " + importe + " €");
        }
        return importe;
    }

    /**
     * Copia los datos de la gasolinera al repostaje y calcula la media provincial.
     *
     * Los datos se CONGELAN (nombre y municipio, no solo el id) porque el catálogo es una
     * foto de hoy: si esa gasolinera cierra, desaparece del listado del Ministerio y el
     * historial se quedaría con huecos. Ver {@link Repostaje}.
     *
     * Si no se eligió gasolinera, o la elegida ya no está en el catálogo, se limpian los
     * tres campos: es preferible un repostaje sin gasolinera que uno con datos de otra.
     */
    private void aplicarEstacion(Repostaje repostaje, String estacionId, Carburante carburante) {
        Optional<Estacion> estacion = estacionId == null
                ? Optional.empty()
                : catalogo.porId(estacionId);

        if (estacion.isEmpty()) {
            repostaje.setEstacionId(null);
            repostaje.setEstacionNombre(null);
            repostaje.setEstacionMunicipio(null);
            repostaje.setEstacionDireccion(null);
            repostaje.setPrecioMedioProvincia(null);
            return;
        }

        Estacion elegida = estacion.get();
        repostaje.setEstacionId(elegida.id());
        repostaje.setEstacionNombre(elegida.rotulo());
        repostaje.setEstacionMunicipio(elegida.municipio());
        repostaje.setEstacionDireccion(elegida.direccion());
        repostaje.setPrecioMedioProvincia(
                precioMedioProvincia(elegida.provincia(), carburante).orElse(null));

        Optional<Zona> zona = zona(elegida, carburante);
        repostaje.setPrecioMedioZona(zona.map(Zona::media).orElse(null));
        repostaje.setPrecioMinimoZona(zona.map(Zona::minimo).orElse(null));
        repostaje.setRadioZonaKm(zona.map(Zona::radioKm).orElse(null));
    }

    /**
     * Cuánto se ha ahorrado (o pagado de más) frente a la referencia de cada repostaje.
     *
     * Solo cuentan los repostajes que tienen referencia guardada: los registrados sin
     * elegir gasolinera no se pueden comparar con nada.
     */
    private BigDecimal ahorroFrenteALaMedia(List<Repostaje> historial) {
        return historial.stream()
                .map(r -> diferenciaFrenteAlaMedia(r))
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Lo que se podría haber ahorrado yendo cada vez a la más barata que había cerca.
     *
     * Nunca es negativo: si el precio apuntado sale por debajo del mínimo del catálogo
     * —pasa cuando la gasolinera cambió de precio ese mismo día—, el repostaje cuenta como
     * "no dejaste nada", que es lo honesto. Fingir un ahorro negativo sería inventarse un
     * dinero que nadie perdió.
     */
    private BigDecimal dejadoEnLaMesa(List<Repostaje> historial) {
        return historial.stream()
                .map(r -> dejadoEnLaMesa(r))
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * (referencia − pagado) × litros. Positivo si salió más barato que la referencia.
     *
     * LA REFERENCIA ES LA ZONA CUANDO EXISTE Y LA PROVINCIA CUANDO NO, y esa segunda rama
     * es solo para el histórico: los repostajes anteriores a que se guardara la zona no
     * pueden recalcularla —no hay precios de aquel día— y quedarse sin comparación sería
     * peor que compararlos con lo que ya tenían.
     */
    private BigDecimal diferenciaFrenteAlaMedia(Repostaje r) {
        BigDecimal referencia = r.getPrecioMedioZona() != null
                ? r.getPrecioMedioZona()
                : r.getPrecioMedioProvincia();

        return referencia == null
                ? null
                : referencia.subtract(r.getPrecioLitro()).multiply(r.getLitros())
                        .setScale(2, RoundingMode.HALF_UP);
    }

    /** Lo que costó no haber ido a la más barata de la zona. Ver {@link #dejadoEnLaMesa(List)}. */
    private BigDecimal dejadoEnLaMesa(Repostaje r) {
        return r.getPrecioMinimoZona() == null
                ? null
                : r.getPrecioLitro().subtract(r.getPrecioMinimoZona()).multiply(r.getLitros())
                        .setScale(2, RoundingMode.HALF_UP)
                        .max(BigDecimal.ZERO);
    }

    /**
     * Qué había alrededor de esa gasolinera: la media y la más barata.
     *
     * ESTO SUSTITUYE A LA MEDIA PROVINCIAL COMO REFERENCIA, y el motivo es que la
     * provincial no significaba nada. Mezcla gasolineras que nunca fueron una alternativa
     * —una a noventa kilómetros no compite con la de tu barrio— y, al ser media aritmética
     * simple, las muchísimas low-cost pequeñas tiran de ella hacia abajo: casi nadie
     * "ahorraba" frente a ella ni yendo a la más barata de su calle.
     */
    private Optional<Zona> zona(Estacion centro, Carburante carburante) {
        List<BigDecimal> precios = preciosCerca(centro, carburante, RADIO_ZONA_KM);
        int radio = RADIO_ZONA_KM;

        // Donde hay poco que elegir se ensancha el círculo. Sin esto, en un pueblo con una
        // sola gasolinera el usuario sería siempre "la más barata de la zona" —él mismo— y
        // el dato no diría nada.
        if (precios.size() < MINIMO_ESTACIONES_ZONA) {
            precios = preciosCerca(centro, carburante, RADIO_ZONA_AMPLIADO_KM);
            radio = RADIO_ZONA_AMPLIADO_KM;
        }

        // Ni ampliando hay con qué comparar: mejor sin zona que con una media de dos.
        if (precios.size() < MINIMO_ESTACIONES_ZONA) {
            return Optional.empty();
        }

        BigDecimal media = precios.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(precios.size()), 3, RoundingMode.HALF_UP);

        return Optional.of(new Zona(media, precios.stream().min(Comparator.naturalOrder()).orElseThrow(), radio));
    }

    /**
     * Precios de ese carburante a menos de {@code radioKm} de la estación, ella incluida.
     *
     * Se incluye a sí misma a propósito: es una de las opciones que había y su precio es
     * tan real como el de las demás. Excluirla haría que "la más barata de la zona" nunca
     * pudiera ser donde repostaste.
     */
    private List<BigDecimal> preciosCerca(Estacion centro, Carburante carburante, double radioKm) {
        // La caja descarta el 99% sin tocar el haversine, igual que en el buscador.
        Geo.CajaBusqueda caja = Geo.caja(centro.latitud(), centro.longitud(), radioKm);

        return catalogo.todas().stream()
                .filter(e -> caja.contiene(e.latitud(), e.longitud()))
                .filter(e -> Geo.distanciaKm(centro.latitud(), centro.longitud(),
                        e.latitud(), e.longitud()) <= radioKm)
                .map(e -> e.precios().get(carburante))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /** Lo que había alrededor de una gasolinera, congelado en el repostaje. */
    private record Zona(BigDecimal media, BigDecimal minimo, int radioKm) {
    }

    /**
     * Precio medio de un carburante en una provincia, ahora mismo.
     *
     * Sale del catálogo en memoria, así que no cuesta ninguna consulta. Se calcula al
     * guardar el repostaje porque este número solo se puede saber ESE día: el Ministerio
     * publica el precio actual, no el histórico.
     */
    private Optional<BigDecimal> precioMedioProvincia(String provincia, Carburante carburante) {
        List<BigDecimal> precios = catalogo.todas().stream()
                .filter(e -> e.provincia().equalsIgnoreCase(provincia))
                .map(e -> e.precios().get(carburante))
                .filter(java.util.Objects::nonNull)
                .toList();

        if (precios.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(precios.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(precios.size()), 3, RoundingMode.HALF_UP));
    }

    private List<GastoMensualDto> gastoMensual(List<Repostaje> historial) {
        // LinkedHashMap para conservar el orden cronológico que ya trae el historial.
        Map<String, GastoMensualDto> porMes = new LinkedHashMap<>();

        historial.stream()
                .sorted(Comparator.comparing(Repostaje::getFecha))
                .forEach(r -> porMes.merge(
                        r.getFecha().format(MES),
                        new GastoMensualDto(r.getFecha().format(MES), r.getImporte(), r.getLitros()),
                        (a, b) -> new GastoMensualDto(a.mes(),
                                a.importe().add(b.importe()),
                                a.litros().add(b.litros()))));

        return List.copyOf(porMes.values());
    }

    private Map<String, String> nombresDeVehiculos(java.util.UUID hogarId) {
        return vehiculos.findByHogarIdAndActivoTrueOrderByCreatedAtAsc(hogarId).stream()
                .collect(java.util.stream.Collectors.toMap(Vehiculo::getId, Vehiculo::getNombre));
    }

    private Vehiculo exigirVehiculo(java.util.UUID hogarId, String vehiculoId) {
        // Por id Y usuario: pasar el id del coche de otro devuelve 404, no sus datos.
        return vehiculos.findByIdAndHogarId(vehiculoId, hogarId)
                .orElseThrow(() -> AppException.notFound("Ese vehículo no existe"));
    }

    private static BigDecimal suma(List<Repostaje> historial,
                                   java.util.function.Function<Repostaje, BigDecimal> campo) {
        return historial.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private VehiculoDto aDto(Vehiculo vehiculo) {
        List<Repostaje> historial = repostajes.findByVehiculoIdOrderByFechaAsc(vehiculo.getId());

        return new VehiculoDto(
                vehiculo.getId(),
                vehiculo.getNombre(),
                vehiculo.getCarburante().name(),
                vehiculo.getCarburante().etiqueta(),
                vehiculo.getMatricula(),
                CalculadoraConsumo.consumoMedio(historial),
                historial.size(),
                suma(historial, Repostaje::getImporte));
    }

    /**
     * La guardada; si el repostaje es de antes de guardarla, la del catálogo de hoy. Una
     * gasolinera no se muda, así que es la misma; si ya cerró, se queda sin dirección.
     */
    private String direccion(Repostaje r) {
        if (r.getEstacionDireccion() != null || r.getEstacionId() == null) {
            return r.getEstacionDireccion();
        }
        return catalogo.porId(r.getEstacionId()).map(Estacion::direccion).orElse(null);
    }

    private RepostajeDto aDto(Repostaje r, String nombreVehiculo) {
        return new RepostajeDto(
                r.getId(),
                r.getVehiculoId(),
                nombreVehiculo,
                r.getFecha(),
                r.getEstacionId(),
                r.getEstacionNombre(),
                r.getEstacionMunicipio(),
                direccion(r),
                r.getCarburante().name(),
                r.getCarburante().etiqueta(),
                r.getLitros(),
                r.getPrecioLitro(),
                r.getImporte(),
                r.getKilometros(),
                r.isDepositoLleno(),
                r.getPrecioMedioProvincia(),
                r.getPrecioMedioZona(),
                r.getPrecioMinimoZona(),
                r.getRadioZonaKm(),
                diferenciaFrenteAlaMedia(r),
                dejadoEnLaMesa(r),
                r.getNotas());
    }
}
