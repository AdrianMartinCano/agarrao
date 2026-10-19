package dev.pimon.listillo.gasolina.estaciones.dto;

import java.math.BigDecimal;

/**
 * Cómo está el precio en la zona que se acaba de buscar.
 *
 * Es lo que convierte una lista de precios en una decisión. Llegas en reserva, tienes
 * delante una a 1,52 y sin referencia no sabes si eso es caro o barato: con la media al
 * lado lo resuelves de un vistazo, y con el mínimo sabes cuánto te ahorras aguantando
 * hasta la siguiente.
 *
 * <h2>Por qué se calcula aquí y no en el navegador</h2>
 *
 * Los resultados van paginados de veinte en veinte. Si la media la calculara el front
 * saldría la de la página que se está viendo, y <b>cambiaría al pasar a la siguiente</b>:
 * un número de referencia que baila es peor que no tener ninguno. Aquí se calcula sobre
 * el conjunto entero y sale gratis, porque la lista completa ya está construida antes de
 * paginar.
 *
 * @param medio       media de la zona buscada, con tres decimales como los surtidores
 * @param minimo      lo que cuesta en la más barata de todas las encontradas
 * @param estaciones  cuántas se han promediado: con dos, una "media" no dice gran cosa
 */
public record ResumenPreciosDto(BigDecimal medio, BigDecimal minimo, int estaciones) {
}
