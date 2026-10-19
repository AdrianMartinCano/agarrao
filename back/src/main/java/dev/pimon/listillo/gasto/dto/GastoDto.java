package dev.pimon.listillo.gasto.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * El gasto de un mes y la tendencia de los meses anteriores.
 *
 * @param total        lo que suman los tickets del mes (con sus descuentos ya restados) y lo anotado
 * @param tickets      tickets del súper del mes
 * @param anotados     gastos anotados del mes (restaurantes, suscripciones…)
 * @param repostajes   repostajes del cuaderno de gasolina del mes (cuentan como GASOLINA)
 * @param porTipo      el total por tipo, de mayor a menor; los tickets cuentan como SUPER y los
 *                     repostajes como GASOLINA
 * @param descuentos   lo que restaron los descuentos del mes, en positivo
 * @param meses        seis meses, del más viejo al más nuevo: hasta el de hoy si el elegido cabe; si no, hasta el elegido
 * @param porCategoria de las líneas con importe positivo; categoría null = sin clasificar
 * @param top          los genéricos en los que más se ha gastado este mes (5 como mucho)
 * @param referencia   el mes más caro de los últimos doce hasta hoy: la vara fija con la que
 *                     se dibuja lo lleno que está cada mes, mires el mes que mires
 */
public record GastoDto(YearMonth mes, BigDecimal total, int tickets, int anotados, int repostajes, BigDecimal descuentos,
                       BigDecimal referencia, List<MesDto> meses, List<TipoGastoDto> porTipo,
                       List<TiendaGastoDto> porTienda, List<CategoriaGastoDto> porCategoria,
                       List<GenericoGastoDto> top) {
}
