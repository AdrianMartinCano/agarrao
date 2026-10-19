package dev.pimon.listillo.ticket;

import dev.pimon.common.exception.AppException;
import dev.pimon.listillo.hogar.AccesoHogar;
import dev.pimon.listillo.lista.ArticuloLista;
import dev.pimon.listillo.lista.ArticuloListaRepository;
import dev.pimon.listillo.producto.Ean;
import dev.pimon.listillo.ticket.dto.BorradorDto;
import dev.pimon.listillo.ticket.dto.GuardadoDto;
import dev.pimon.listillo.ticket.dto.GuardarTicketRequest;
import dev.pimon.listillo.ticket.dto.LineaBorradorDto;
import dev.pimon.listillo.ticket.dto.LineaRequest;
import dev.pimon.listillo.ticket.dto.LineaTicketDto;
import dev.pimon.listillo.ticket.dto.TicketDto;
import dev.pimon.listillo.ticket.dto.TicketResumenDto;
import dev.pimon.listillo.ticket.lectura.InterpreteTicket;
import dev.pimon.listillo.ticket.lectura.TextoDePdf;
import dev.pimon.listillo.ticket.lectura.TicketLeido;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tickets de un hogar: leer (PDF o texto de una foto) → repasar → guardar.
 *
 * Leer no guarda nada: devuelve un borrador con cada línea y su propuesta de casado. Al
 * guardar, lo casado enseña al hogar (alias por cadena) y tacha de la lista lo comprado.
 */
@Service
@RequiredArgsConstructor
public class TicketService {

    /** El listado enseña los últimos; los gráficos irán por su propia consulta agregada. */
    static final int ULTIMOS = 100;

    private final AccesoHogar acceso;
    private final TicketRepository tickets;
    private final AliasTicketRepository aliases;
    private final ArticuloListaRepository articulos;
    private final ApplicationEventPublisher eventos;

    @Transactional(readOnly = true)
    public BorradorDto leerPdf(UUID hogarId, String usuarioId, byte[] pdf) {
        acceso.exigirMiembro(hogarId, usuarioId);
        return borrador(hogarId, TextoDePdf.extraer(pdf), OrigenTicket.PDF);
    }

    @Transactional(readOnly = true)
    public BorradorDto leerTexto(UUID hogarId, String usuarioId, String texto) {
        acceso.exigirMiembro(hogarId, usuarioId);
        return borrador(hogarId, texto, OrigenTicket.FOTO);
    }

    @Transactional
    public GuardadoDto guardar(UUID hogarId, String usuarioId, GuardarTicketRequest peticion) {
        acceso.exigirMiembro(hogarId, usuarioId);
        for (LineaRequest l : peticion.lineas()) {
            if (l.ean() != null && !Ean.esValido(l.ean())) {
                throw AppException.badRequest("Ese código de barras no es válido: " + l.ean());
            }
        }
        BigDecimal total = peticion.total() != null ? peticion.total()
                : peticion.lineas().stream().map(LineaRequest::importe).reduce(BigDecimal.ZERO, BigDecimal::add);

        Ticket ticket = new Ticket(hogarId, peticion.cadena(), vacioANull(peticion.tiendaTexto()),
                peticion.fecha(), total, peticion.origen(), usuarioId);
        for (LineaRequest l : peticion.lineas()) {
            String nombre = vacioANull(l.nombre());
            ticket.anadir(new LineaTicket(l.texto().strip(), l.cantidad(), l.unidad(), l.precioUnitario(),
                    l.importe(), nombre == null ? null : l.ean(), nombre));
        }
        tickets.save(ticket);

        aprender(ticket);
        int tachados = tacharDeLaLista(hogarId, usuarioId, ticket.getLineas());
        // Los genéricos de los productos nuevos se adivinan al confirmar (Clasificador).
        eventos.publishEvent(new TicketGuardado(hogarId));
        return new GuardadoDto(aDto(ticket), tachados);
    }

    @Transactional(readOnly = true)
    public List<TicketResumenDto> listar(UUID hogarId, String usuarioId) {
        acceso.exigirMiembro(hogarId, usuarioId);
        List<Ticket> ultimos = tickets.findByHogarIdOrderByFechaDescCreadoEnDesc(hogarId, PageRequest.of(0, ULTIMOS));
        if (ultimos.isEmpty()) {
            return List.of();
        }
        Map<UUID, ConteoLineas> conteos = tickets.contarLineas(ultimos.stream().map(Ticket::getId).toList())
                .stream().collect(Collectors.toMap(ConteoLineas::ticketId, Function.identity()));
        return ultimos.stream().map(t -> {
            ConteoLineas c = conteos.getOrDefault(t.getId(), new ConteoLineas(t.getId(), 0, 0));
            return new TicketResumenDto(t.getId(), t.getCadena(), t.getFecha(), t.getTotal(), c.lineas(), c.casadas());
        }).toList();
    }

    @Transactional(readOnly = true)
    public TicketDto detalle(UUID hogarId, UUID ticketId, String usuarioId) {
        acceso.exigirMiembro(hogarId, usuarioId);
        return aDto(buscar(hogarId, ticketId));
    }

    @Transactional
    public void borrar(UUID hogarId, UUID ticketId, String usuarioId) {
        acceso.exigirMiembro(hogarId, usuarioId);
        tickets.delete(buscar(hogarId, ticketId));
    }

    private BorradorDto borrador(UUID hogarId, String texto, OrigenTicket origen) {
        TicketLeido leido = InterpreteTicket.leer(texto);
        Map<String, AliasTicket> aprendidos = aprendidos(hogarId, leido);
        Collection<Candidato> candidatos = candidatos(hogarId);

        List<LineaBorradorDto> lineas = leido.lineas().stream()
                .map(l -> new LineaBorradorDto(l.texto(), l.cantidad(), l.unidad(), l.precioUnitario(), l.importe(),
                        Casador.sugerir(l.texto(), aprendidos, candidatos).orElse(null)))
                .toList();
        return new BorradorDto(leido.cadena(), leido.tiendaTexto(), leido.fecha(), leido.total(), origen,
                lineas, leido.avisos(), texto);
    }

    /** Lo aprendido por el hogar en esta cadena para los textos de este ticket, en una consulta. */
    private Map<String, AliasTicket> aprendidos(UUID hogarId, TicketLeido leido) {
        if (leido.cadena() == Cadena.OTRA) {
            return Map.of();
        }
        List<AliasTicket.Clave> claves = leido.lineas().stream()
                .map(l -> new AliasTicket.Clave(hogarId, leido.cadena(), Textos.normalizar(l.texto())))
                .distinct().toList();
        return aliases.findAllById(claves).stream()
                .collect(Collectors.toMap(a -> a.getClave().getTexto(), Function.identity()));
    }

    /** Lo que hay o ha habido en la lista del hogar y lo casado en sus tickets anteriores. */
    private Collection<Candidato> candidatos(UUID hogarId) {
        Set<Candidato> candidatos = new LinkedHashSet<>(tickets.casadasDelHogar(hogarId));
        for (ArticuloLista a : articulos.findByHogarId(hogarId)) {
            candidatos.add(new Candidato(a.getEan(), a.getNombre()));
        }
        return candidatos;
    }

    /** Solo en cadenas conocidas: en «otra tienda» el mismo texto puede ser cualquier cosa. */
    private void aprender(Ticket ticket) {
        if (ticket.getCadena() == Cadena.OTRA) {
            return;
        }
        Map<String, LineaTicket> casadas = new HashMap<>();
        for (LineaTicket l : ticket.getLineas()) {
            if (l.casada()) {
                casadas.put(Textos.normalizar(l.getTexto()), l);
            }
        }
        List<AliasTicket.Clave> claves = casadas.keySet().stream()
                .map(t -> new AliasTicket.Clave(ticket.getHogarId(), ticket.getCadena(), t)).toList();
        Map<String, AliasTicket> existentes = aliases.findAllById(claves).stream()
                .collect(Collectors.toMap(a -> a.getClave().getTexto(), Function.identity()));

        List<AliasTicket> aGuardar = new ArrayList<>();
        casadas.forEach((texto, linea) -> {
            if (texto.isEmpty()) {
                return;
            }
            AliasTicket alias = existentes.get(texto);
            if (alias == null) {
                aGuardar.add(new AliasTicket(ticket.getHogarId(), ticket.getCadena(), texto, linea.getEan(), linea.getNombre()));
            } else {
                alias.confirmar(linea.getEan(), linea.getNombre());
            }
        });
        aliases.saveAll(aGuardar);
    }

    /** Lo pendiente de la lista que sale en el ticket (mismo código o mismo nombre) se tacha. */
    private int tacharDeLaLista(UUID hogarId, String usuarioId, List<LineaTicket> lineas) {
        Set<String> eans = lineas.stream().map(LineaTicket::getEan).filter(e -> e != null).collect(Collectors.toSet());
        Set<String> nombres = lineas.stream().filter(LineaTicket::casada)
                .map(l -> Textos.normalizar(l.getNombre())).collect(Collectors.toSet());
        int tachados = 0;
        for (ArticuloLista a : articulos.findByHogarIdAndCompradoFalse(hogarId)) {
            if ((a.getEan() != null && eans.contains(a.getEan())) || nombres.contains(Textos.normalizar(a.getNombre()))) {
                a.marcarComprado(true, usuarioId);
                tachados++;
            }
        }
        return tachados;
    }

    private Ticket buscar(UUID hogarId, UUID ticketId) {
        return tickets.findWithLineasByIdAndHogarId(ticketId, hogarId)
                .orElseThrow(() -> AppException.notFound("Ese ticket no existe"));
    }

    private static TicketDto aDto(Ticket t) {
        return new TicketDto(t.getId(), t.getCadena(), t.getTiendaTexto(), t.getFecha(), t.getTotal(), t.getOrigen(),
                t.getLineas().stream()
                        .map(l -> new LineaTicketDto(l.getId(), l.getTexto(), l.getCantidad(), l.getUnidad(),
                                l.getPrecioUnitario(), l.getImporte(), l.getEan(), l.getNombre()))
                        .toList());
    }

    private static String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
