package dev.pimon.listillo;

import dev.pimon.listillo.hogar.HogarService;
import dev.pimon.listillo.hogar.dto.HogarDto;
import dev.pimon.listillo.lista.ListaService;
import dev.pimon.listillo.lista.dto.NuevoArticuloRequest;
import dev.pimon.listillo.ticket.Cadena;
import dev.pimon.listillo.ticket.OrigenTicket;
import dev.pimon.listillo.ticket.TicketService;
import dev.pimon.listillo.ticket.Unidad;
import dev.pimon.listillo.ticket.dto.GuardarTicketRequest;
import dev.pimon.listillo.ticket.dto.LineaRequest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Un hogar de ejemplo para el usuario de prueba, con siete meses de tickets inventados de
 * cuatro cadenas: para ver Precios, Gasto (la hucha llena) y los gráficos en local sin subir
 * nada a mano.
 *
 * Se rehace en cada arranque de {@link ListilloLocal} con fechas contadas desde hoy, para que
 * el mes actual nunca esté vacío. Solo toca el hogar {@value #NOMBRE}: el resto de datos de
 * la base local se queda como estaba. Datos inventados, con la forma de los tickets reales.
 */
final class DatosDeEjemplo {

    static final String NOMBRE = "Casa de ejemplo";
    private static final int DIAS = 210;

    /** Un producto y cómo se llama (y cuánto cuesta) en cada cadena. A peso: kilos mín.–máx. */
    private record Articulo(Map<Cadena, Precio> precios, double kgMin, double kgMax) {
        Articulo(Map<Cadena, Precio> precios) {
            this(precios, 0, 0);
        }

        boolean aPeso() {
            return kgMax > 0;
        }
    }

    private record Precio(String texto, double euros) {
    }

    private static Precio p(String texto, double euros) {
        return new Precio(texto, euros);
    }

    private static final List<Articulo> SEMANALES = List.of(
            new Articulo(Map.of(Cadena.MERCADONA, p("LECHE SEMI HACEND 6X1L", 5.34), Cadena.LIDL, p("LECHE SEMIDESNATADA MILBONA 6X1L", 5.10),
                    Cadena.CARREFOUR, p("LECHE SEMI CARREFOUR 6X1L", 5.70), Cadena.DIA, p("LECHE SEMIDESNATADA DIA 6X1L", 5.04))),
            new Articulo(Map.of(Cadena.MERCADONA, p("HUEVOS L 12 UDS", 2.65), Cadena.LIDL, p("HUEVOS CAMPEROS DOCENA", 3.19),
                    Cadena.CARREFOUR, p("HUEVOS L 12 UDS CARREFOUR", 2.89), Cadena.DIA, p("HUEVOS M DIA 12 UDS", 2.35))),
            new Articulo(Map.of(Cadena.MERCADONA, p("PLATANO CANARIAS", 1.99), Cadena.LIDL, p("PLATANOS", 1.89),
                    Cadena.CARREFOUR, p("PLATANO DE CANARIAS", 2.19), Cadena.DIA, p("PLATANO", 1.95)), 0.6, 1.4),
            new Articulo(Map.of(Cadena.MERCADONA, p("PAN DE MOLDE INTEGRAL 450G", 1.25), Cadena.LIDL, p("PAN MOLDE INTEGRAL 500G", 1.19),
                    Cadena.CARREFOUR, p("PAN DE MOLDE 460G", 1.39), Cadena.DIA, p("PAN DE MOLDE DIA 450G", 1.09))),
            new Articulo(Map.of(Cadena.MERCADONA, p("YOGUR NATURAL 4X125G", 0.95), Cadena.LIDL, p("YOGUR NATURAL PILOS 4X125G", 0.85),
                    Cadena.CARREFOUR, p("YOGUR NATURAL 6X125G", 1.62), Cadena.DIA, p("YOGUR NATURAL DIA 4X125G", 0.79))),
            new Articulo(Map.of(Cadena.MERCADONA, p("TOMATE PERA", 2.29), Cadena.LIDL, p("TOMATE RAMA", 2.49),
                    Cadena.CARREFOUR, p("TOMATE PERA", 2.39), Cadena.DIA, p("TOMATE ENSALADA", 1.99)), 0.5, 1.2),
            new Articulo(Map.of(Cadena.MERCADONA, p("PECHUGA POLLO FILETES", 7.10), Cadena.LIDL, p("PECHUGA DE POLLO", 6.99),
                    Cadena.CARREFOUR, p("FILETE PECHUGA POLLO", 7.49), Cadena.DIA, p("PECHUGA POLLO FILETEADA", 6.79)), 0.5, 1.1),
            new Articulo(Map.of(Cadena.MERCADONA, p("MANZANA GOLDEN", 1.75), Cadena.LIDL, p("MANZANA GOLDEN", 1.60),
                    Cadena.CARREFOUR, p("MANZANA GALA", 1.99), Cadena.DIA, p("MANZANA GOLDEN", 1.69)), 0.8, 1.6),
            new Articulo(Map.of(Cadena.MERCADONA, p("AGUA MINERAL 6X1,5L", 1.62), Cadena.LIDL, p("AGUA SALIENTE 6X1,5L", 1.45),
                    Cadena.CARREFOUR, p("AGUA MINERAL 6X1,5L", 1.80), Cadena.DIA, p("AGUA MINERAL DIA 6X1,5L", 1.39))),
            new Articulo(Map.of(Cadena.MERCADONA, p("PATATA MALLA 3KG", 3.49), Cadena.LIDL, p("PATATAS 3KG", 3.29),
                    Cadena.CARREFOUR, p("PATATA MALLA 3KG", 3.79), Cadena.DIA, p("PATATA DIA 3KG", 3.19))));

    private static final List<Articulo> A_VECES = List.of(
            new Articulo(Map.of(Cadena.MERCADONA, p("ACEITE OLIVA V EXTRA 1L", 8.95), Cadena.LIDL, p("ACEITE DE OLIVA VIRGEN EXTRA 1L", 8.49),
                    Cadena.CARREFOUR, p("ACEITE OLIVA VIRGEN EXTRA 1L", 9.29), Cadena.DIA, p("ACEITE OLIVA V EXTRA DIA 1L", 8.79))),
            new Articulo(Map.of(Cadena.MERCADONA, p("TOMATE FRITO HACENDADO 400G", 0.85), Cadena.LIDL, p("TOMATE FRITO 3X400G", 2.19),
                    Cadena.CARREFOUR, p("TOMATE FRITO 400G", 0.95), Cadena.DIA, p("TOMATE FRITO DIA 400G", 0.79))),
            new Articulo(Map.of(Cadena.MERCADONA, p("MACARRONES 1KG", 1.10), Cadena.LIDL, p("MACARRONES COMBINO 1KG", 0.99),
                    Cadena.CARREFOUR, p("MACARRONES 1KG", 1.25), Cadena.DIA, p("MACARRONES DIA 1KG", 0.95))),
            new Articulo(Map.of(Cadena.MERCADONA, p("ARROZ REDONDO 1KG", 1.35), Cadena.LIDL, p("ARROZ REDONDO 1KG", 1.29),
                    Cadena.CARREFOUR, p("ARROZ REDONDO 1KG", 1.45), Cadena.DIA, p("ARROZ REDONDO DIA 1KG", 1.19))),
            new Articulo(Map.of(Cadena.MERCADONA, p("PAPEL HIGIENICO 12 ROLLOS", 4.40), Cadena.LIDL, p("PAPEL HIGIENICO FLORALYS 12 ROLLOS", 3.99),
                    Cadena.CARREFOUR, p("PAPEL HIGIENICO 12 ROLLOS", 4.75), Cadena.DIA, p("PAPEL HIGIENICO DIA 12 ROLLOS", 3.89))),
            new Articulo(Map.of(Cadena.MERCADONA, p("DETERGENTE ROPA 2L", 4.50), Cadena.LIDL, p("DETERGENTE FORMIL 2L", 3.99),
                    Cadena.CARREFOUR, p("DETERGENTE LIQUIDO 2L", 4.95), Cadena.DIA, p("DETERGENTE DIA 2L", 3.79))),
            new Articulo(Map.of(Cadena.MERCADONA, p("CAFE MOLIDO 250G", 2.45), Cadena.LIDL, p("CAFE MOLIDO BELLAROM 250G", 2.29),
                    Cadena.CARREFOUR, p("CAFE MOLIDO 250G", 2.69), Cadena.DIA, p("CAFE MOLIDO DIA 250G", 2.19))),
            new Articulo(Map.of(Cadena.MERCADONA, p("GALLETAS MARIA 800G", 1.65), Cadena.LIDL, p("GALLETAS MARIA 800G", 1.49),
                    Cadena.CARREFOUR, p("GALLETAS MARIA 800G", 1.79), Cadena.DIA, p("GALLETAS MARIA DIA 800G", 1.45))),
            new Articulo(Map.of(Cadena.MERCADONA, p("CERVEZA LATA 12X33CL", 6.60), Cadena.LIDL, p("CERVEZA PERLENBACHER 12X33CL", 5.88),
                    Cadena.CARREFOUR, p("CERVEZA LATA 12X33CL", 6.99), Cadena.DIA, p("CERVEZA DIA 12X33CL", 5.69))),
            new Articulo(Map.of(Cadena.MERCADONA, p("SALMON FILETE", 19.95), Cadena.LIDL, p("SALMON FILETES", 18.99),
                    Cadena.CARREFOUR, p("SALMON FRESCO", 21.50), Cadena.DIA, p("SALMON FILETE", 19.49)), 0.3, 0.6),
            new Articulo(Map.of(Cadena.MERCADONA, p("JAMON COCIDO EXTRA 200G", 2.20), Cadena.LIDL, p("JAMON COCIDO 200G", 1.99),
                    Cadena.CARREFOUR, p("JAMON YORK 200G", 2.39), Cadena.DIA, p("JAMON COCIDO DIA 200G", 1.89))),
            new Articulo(Map.of(Cadena.MERCADONA, p("PIZZA 4 QUESOS 400G", 2.80), Cadena.LIDL, p("PIZZA JAMON QUESO 390G", 1.99),
                    Cadena.CARREFOUR, p("PIZZA BARBACOA 400G", 3.10), Cadena.DIA, p("PIZZA 4 QUESOS DIA 400G", 2.49))),
            new Articulo(Map.of(Cadena.MERCADONA, p("GEL DUCHA 750ML", 1.60), Cadena.LIDL, p("GEL DUCHA CIEN 750ML", 1.39),
                    Cadena.CARREFOUR, p("GEL DUCHA 750ML", 1.95), Cadena.DIA, p("GEL DUCHA DIA 750ML", 1.35))),
            new Articulo(Map.of(Cadena.MERCADONA, p("NARANJA MESA", 1.70), Cadena.LIDL, p("NARANJAS", 1.59),
                    Cadena.CARREFOUR, p("NARANJA DE MESA", 1.89), Cadena.DIA, p("NARANJA", 1.65)), 1.0, 2.5),
            new Articulo(Map.of(Cadena.MERCADONA, p("CARNE PICADA 500G", 3.60), Cadena.LIDL, p("CARNE PICADA MIXTA 500G", 3.29),
                    Cadena.CARREFOUR, p("CARNE PICADA 500G", 3.89), Cadena.DIA, p("CARNE PICADA DIA 500G", 3.49))),
            new Articulo(Map.of(Cadena.MERCADONA, p("CHOCOLATE NEGRO 200G", 1.45), Cadena.LIDL, p("CHOCOLATE NEGRO FIN CARRE 200G", 1.29),
                    Cadena.CARREFOUR, p("CHOCOLATE NEGRO 200G", 1.59), Cadena.DIA, p("CHOCOLATE NEGRO DIA 200G", 1.25))),
            new Articulo(Map.of(Cadena.MERCADONA, p("LEJIA 2L", 1.10), Cadena.LIDL, p("LEJIA W5 2L", 0.99),
                    Cadena.CARREFOUR, p("LEJIA 2L", 1.25), Cadena.DIA, p("LEJIA DIA 2L", 0.95))));

    private static final List<String> LISTA = List.of(
            "Leche semidesnatada", "Huevos", "Plátanos", "Aceite de oliva virgen extra", "Pan de molde",
            "Tomate frito", "Papel higiénico", "Café molido", "Yogur natural", "Pechuga de pollo");

    private DatosDeEjemplo() {
    }

    static void rehacer(ConfigurableApplicationContext contexto, String email) {
        JdbcTemplate jdbc = contexto.getBean(JdbcTemplate.class);
        List<String> ids = jdbc.queryForList("select id from app_users where email = ?", String.class, email);
        if (ids.isEmpty()) {
            return;
        }
        String yo = ids.getFirst();
        jdbc.update("""
                delete from hogares where nombre = ? and id in (select hogar_id from miembros_hogar where usuario_id = ?)
                """, NOMBRE, yo);

        HogarDto hogar = contexto.getBean(HogarService.class).crear(yo, NOMBRE);
        TicketService tickets = contexto.getBean(TicketService.class);
        Random azar = new Random(42);
        LocalDate hoy = LocalDate.now();
        int guardados = 0;
        for (LocalDate dia = hoy.minusDays(DIAS); !dia.isAfter(hoy); dia = dia.plusDays(1)) {
            Cadena cadena = cadenaDelDia(dia, azar);
            if (cadena == null) {
                continue;
            }
            // Los precios suben un poco con los meses (un 0,4 % al mes).
            double inflacion = 1 + 0.004 * (DIAS - (hoy.toEpochDay() - dia.toEpochDay())) / 30.0;
            List<LineaRequest> lineas = new ArrayList<>();
            int semanales = cadena == Cadena.MERCADONA ? 6 + azar.nextInt(4) : 2 + azar.nextInt(4);
            for (Articulo a : elegir(SEMANALES, semanales, azar)) {
                lineas.add(linea(a, cadena, inflacion, azar));
            }
            for (Articulo a : elegir(A_VECES, 1 + azar.nextInt(cadena == Cadena.MERCADONA ? 5 : 3), azar)) {
                lineas.add(linea(a, cadena, inflacion, azar));
            }
            if (azar.nextInt(6) == 0) {
                lineas.add(new LineaRequest("DESCUENTO 2A UNIDAD", BigDecimal.ONE, Unidad.UD, null,
                        BigDecimal.valueOf(-(0.3 + azar.nextInt(10) / 10.0)).setScale(2, RoundingMode.HALF_UP), null, null));
            }
            BigDecimal total = lineas.stream().map(LineaRequest::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
            tickets.guardar(hogar.id(), yo, new GuardarTicketRequest(cadena, null, dia, total, OrigenTicket.PDF, lineas));
            guardados++;
        }

        ListaService lista = contexto.getBean(ListaService.class);
        for (String cosa : LISTA) {
            lista.apuntar(hogar.id(), yo, new NuevoArticuloRequest(cosa, 1, null));
        }
        System.out.printf("%nDatos de ejemplo: hogar «%s» con %d tickets de los últimos %d días.%n%n", NOMBRE, guardados, DIAS);
    }

    /** Mercadona los sábados, Lidl los martes, Dia alguna vez entre semana, Carrefour un domingo al mes. */
    private static Cadena cadenaDelDia(LocalDate dia, Random azar) {
        if (dia.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return Cadena.MERCADONA;
        }
        if (dia.getDayOfWeek() == DayOfWeek.TUESDAY && azar.nextInt(3) > 0) {
            return Cadena.LIDL;
        }
        if (dia.getDayOfWeek() == DayOfWeek.THURSDAY && azar.nextInt(3) == 0) {
            return Cadena.DIA;
        }
        if (dia.getDayOfWeek() == DayOfWeek.SUNDAY && dia.getDayOfMonth() <= 7) {
            return Cadena.CARREFOUR;
        }
        return null;
    }

    private static List<Articulo> elegir(List<Articulo> de, int cuantos, Random azar) {
        List<Articulo> copia = new ArrayList<>(de);
        java.util.Collections.shuffle(copia, azar);
        return copia.subList(0, Math.min(cuantos, copia.size()));
    }

    private static LineaRequest linea(Articulo a, Cadena cadena, double inflacion, Random azar) {
        Precio precio = a.precios().get(cadena);
        // Un poco de ruido por semana, como las ofertas y los redondeos del súper.
        double euros = precio.euros() * inflacion * (0.97 + azar.nextDouble() * 0.06);
        BigDecimal unitario = BigDecimal.valueOf(euros).setScale(2, RoundingMode.HALF_UP);
        if (a.aPeso()) {
            BigDecimal kilos = BigDecimal.valueOf(a.kgMin() + azar.nextDouble() * (a.kgMax() - a.kgMin()))
                    .setScale(3, RoundingMode.HALF_UP);
            return new LineaRequest(precio.texto(), kilos, Unidad.KG, unitario,
                    kilos.multiply(unitario).setScale(2, RoundingMode.HALF_UP), null, null);
        }
        int veces = azar.nextInt(5) == 0 ? 2 : 1;
        return new LineaRequest(precio.texto(), BigDecimal.valueOf(veces), Unidad.UD, veces > 1 ? unitario : null,
                unitario.multiply(BigDecimal.valueOf(veces)), null, null);
    }
}
