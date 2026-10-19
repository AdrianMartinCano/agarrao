/** Tipos de vehículos y repostajes. Contrato con el backend, escrito a mano. */

export interface Vehiculo {
  id: string;
  nombre: string;
  carburante: string;
  carburanteEtiqueta: string;
  matricula: string | null;
  /** l/100 km. Null hasta que haya dos llenados completos con kilómetros. */
  consumoMedio: number | null;
  repostajes: number;
  gastoTotal: number;
}

export interface Repostaje {
  id: string;
  vehiculoId: string;
  vehiculoNombre: string;
  fecha: string;
  /** Se conserva al editar si no se elige otra gasolinera. */
  estacionId: string | null;
  estacionNombre: string | null;
  estacionMunicipio: string | null;
  estacionDireccion: string | null;
  carburante: string;
  carburanteEtiqueta: string;
  litros: number;
  precioLitro: number;
  importe: number;
  kilometros: number | null;
  depositoLleno: boolean;
  /** Media en la provincia. Solo la traen los repostajes anteriores a la zona. */
  precioMedioProvincia: number | null;
  /** Media alrededor de la gasolinera, dentro de `radioZonaKm`. */
  precioMedioZona: number | null;
  /** Lo que costaba el litro en la más barata de esa zona. */
  precioMinimoZona: number | null;
  /** No es fijo: se ensancha donde hay pocas gasolineras. */
  radioZonaKm: number | null;
  /** Euros ahorrados (positivo) o pagados de más (negativo) frente a la media. */
  diferenciaMedia: number | null;
  /** Lo que habría costado menos yendo a la más barata. Cero = fuiste a ella. */
  dejadoEnLaMesa: number | null;
  notas: string | null;
}

export interface NuevoVehiculo {
  nombre: string;
  carburante: string;
  matricula: string | null;
}

export interface NuevoRepostaje {
  vehiculoId: string;
  fecha: string;
  carburante: string;
  litros: number;
  precioLitro: number;
  /** Lo del ticket: se guarda tal cual (20 € son 20,00 €, sin redondeos de los litros). */
  importe: number | null;
  kilometros: number | null;
  depositoLleno: boolean;
  estacionId: string | null;
  notas: string | null;
}

export interface TramoConsumo {
  hasta: string;
  kilometros: number;
  litros: number;
  consumo: number;
}

export interface GastoMensual {
  mes: string;
  importe: number;
  litros: number;
}

export interface PrecioPagado {
  fecha: string;
  precioLitro: number;
  /** La media de la zona, o la de la provincia en los repostajes antiguos. */
  precioReferencia: number | null;
}

export interface Estadisticas {
  consumoMedio: number | null;
  gastoTotal: number;
  litrosTotales: number;
  kilometrosRecorridos: number;
  ahorroFrenteALaMedia: number;
  /** Lo que se habría ahorrado además yendo siempre a la más barata de la zona. */
  dejadoEnLaMesa: number;
  consumos: TramoConsumo[];
  gastoMensual: GastoMensual[];
  precios: PrecioPagado[];
}
