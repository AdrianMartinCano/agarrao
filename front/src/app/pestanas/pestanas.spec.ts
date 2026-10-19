import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NavController } from '@ionic/angular';

import { Parte, PartesService } from '../core/partes.service';
import { Pestanas } from './pestanas';

describe('Pestanas', () => {
  const partes = signal<readonly Parte[]>(['LISTA', 'PRECIOS', 'GASTOS']);

  function nombres(): string[] {
    const barra = TestBed.createComponent(Pestanas);
    barra.componentRef.setInput('activa', 'lista');
    barra.detectChanges();
    return Array.from(barra.nativeElement.querySelectorAll('.pestana') as NodeListOf<HTMLElement>).map((a) =>
      a.textContent!.trim(),
    );
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: NavController, useValue: { navigateRoot: () => Promise.resolve(true) } },
        { provide: PartesService, useValue: { partes, cargar: () => undefined } },
      ],
    });
  });

  it('el ＋ va en medio cuando están todas', () => {
    partes.set(['LISTA', 'PRECIOS', 'GASTOS']);
    expect(nombres()).toEqual(['Lista', 'Precios', 'Añadir', 'Gasto', 'Hogar']);
  });

  it('lo apagado desaparece; el ＋ y Hogar siempre están', () => {
    partes.set(['GASTOS']);
    expect(nombres()).toEqual(['Añadir', 'Gasto', 'Hogar']);
  });
});
