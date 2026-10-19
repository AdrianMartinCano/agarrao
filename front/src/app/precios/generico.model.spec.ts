import { precioPorUnidad, retrato, tamano } from './generico.model';

describe('generico.model', () => {
  it('escribe el precio por unidad a la española', () => {
    expect(precioPorUnidad(0.89, 'L')).toBe('0,89 €/L');
    expect(precioPorUnidad(1.988, 'KG')).toBe('1,99 €/kg');
  });

  it('no redondea a cero lo que no llega a diez céntimos', () => {
    expect(precioPorUnidad(0.042, 'UD')).toBe('0,042 €/ud');
  });

  it('escribe el tamaño con su unidad', () => {
    expect(tamano(6, 'L')).toBe('6 L');
    expect(tamano(0.5, 'KG')).toBe('0,5 kg');
  });

  it('usa el dibujo propio o, si no, el de su pasillo', () => {
    expect(retrato({ retrato: 'huevo.svg', categoria: 'LACTEOS' })).toBe('personajes/huevo.svg');
    expect(retrato({ retrato: null, categoria: 'FRUTA' })).toBe('personajes/fruta.svg');
    expect(retrato({ retrato: null, categoria: null })).toBe('personajes/listillo.svg');
  });
});
