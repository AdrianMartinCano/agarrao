import { eanValido } from './ean';
import { nombreParaLista } from './productos.service';

describe('eanValido', () => {
  it('acepta EAN-13, EAN-8, UPC-A y GTIN-14 con su dígito de control', () => {
    for (const c of ['8480000107947', '96385074', '036000291452', '10012345678902']) {
      expect(eanValido(c)).toBe(true);
    }
  });

  it('rechaza un dígito de control malo, largos raros y letras', () => {
    for (const c of ['8480000107948', '848000010794', '8480000107947a', '', '1234567']) {
      expect(eanValido(c)).toBe(false);
    }
  });
});

describe('nombreParaLista', () => {
  it('añade la marca si el nombre no la lleva ya', () => {
    expect(nombreParaLista({ nombre: 'Leche semidesnatada', marca: 'Hacendado' })).toBe('Leche semidesnatada Hacendado');
    expect(nombreParaLista({ nombre: 'Nocilla original', marca: 'Nocilla' })).toBe('Nocilla original');
    expect(nombreParaLista({ nombre: 'Pan', marca: null })).toBe('Pan');
  });

  it('no pasa de 80 caracteres, como la lista', () => {
    expect(nombreParaLista({ nombre: 'x'.repeat(100), marca: null })).toHaveLength(80);
  });
});
