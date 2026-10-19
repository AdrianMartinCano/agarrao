import { Injectable, signal } from '@angular/core';

import { FiltrosBusqueda, OrdenBusqueda } from './api.model';

const CLAVE = 'gaspath.filtros';

/** Con lo que arranca alguien que entra por primera vez. */
const POR_DEFECTO: FiltrosBusqueda = {
  // La 95 es, con diferencia, la que más gente busca: es el valor con el que la página
  // resulta útil sin tocar nada.
  carburante: 'GASOLINA_95_E5',
  radioKm: 5,
  orden: 'DISTANCIA',
};

/** Radios que se ofrecen. Se valida contra esta lista lo que venga guardado. */
export const RADIOS: readonly number[] = [1, 2, 5, 10, 25, 50];

/**
 * Los filtros de búsqueda, recordados entre visitas.
 *
 * Vive en un servicio y no en el componente porque el componente se destruye al navegar
 * a favoritas: si el estado estuviera dentro, al volver se habría perdido. Y se guarda
 * además en localStorage para que tampoco se pierda al cerrar el navegador: quien
 * siempre busca en 25 km no tiene por qué volver a elegirlo cada mañana.
 */
@Injectable({ providedIn: 'root' })
export class FiltrosStore {
  private readonly estado = signal<FiltrosBusqueda>(leerGuardados());

  readonly filtros = this.estado.asReadonly();

  actualizar(cambio: (actuales: FiltrosBusqueda) => FiltrosBusqueda): void {
    this.estado.update((actuales) => {
      const nuevos = cambio(actuales);
      guardar(nuevos);
      return nuevos;
    });
  }
}

function guardar(filtros: FiltrosBusqueda): void {
  try {
    localStorage.setItem(CLAVE, JSON.stringify(filtros));
  } catch {
    // Modo incógnito o almacenamiento lleno. No poder recordar la preferencia no es
    // motivo para romper la búsqueda.
  }
}

/**
 * Lee los filtros guardados, validando lo que sale.
 *
 * Lo de localStorage es texto que el usuario puede editar y que además puede venir de
 * una versión anterior de la aplicación, con otros nombres de carburante o radios que ya
 * no existen. Cada campo que no encaje se sustituye por el de por defecto, en vez de
 * dejar que llegue al backend un filtro que devolvería un 400.
 */
function leerGuardados(): FiltrosBusqueda {
  const guardado = localStorage.getItem(CLAVE);
  if (!guardado) {
    return POR_DEFECTO;
  }

  try {
    const leido = JSON.parse(guardado) as Partial<FiltrosBusqueda>;

    const radioKm =
      typeof leido.radioKm === 'number' && RADIOS.includes(leido.radioKm)
        ? leido.radioKm
        : POR_DEFECTO.radioKm;

    const orden: OrdenBusqueda = leido.orden === 'PRECIO' ? 'PRECIO' : 'DISTANCIA';

    const carburante =
      typeof leido.carburante === 'string' && leido.carburante ? leido.carburante : null;

    return {
      carburante,
      radioKm,
      // Ordenar por precio sin carburante da error en el backend: es una combinación que
      // la interfaz no deja formar, pero sí puede quedar guardada de una versión vieja.
      orden: !carburante && orden === 'PRECIO' ? 'DISTANCIA' : orden,
    };
  } catch {
    localStorage.removeItem(CLAVE);
    return POR_DEFECTO;
  }
}
