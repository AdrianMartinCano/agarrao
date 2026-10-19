import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonContent } from '@ionic/angular';

/**
 * Condiciones de uso de Agarrao (las enlaza la pantalla de Google al entrar).
 *
 * Mismo criterio que la privacidad: lo que la app es de verdad. Lo importante son los
 * PRECIOS: no son nuestros (Ministerio, webs de los súper, Open Prices, vuestros tickets),
 * pueden ir con retraso y el de la tienda manda. Los de las webs de los súper son para uso
 * privado de cada casa: por eso se dice que no se pueden sacar en masa.
 */
@Component({
  selector: 'app-condiciones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IonContent, RouterLink],
  template: `
    <ion-content class="trama">
      <article class="documento">
        <a class="documento__volver" routerLink="/">← Agarrao</a>
        <h1 class="titular">Condiciones de uso</h1>
        <p class="actualizado">Última actualización: 13 de octubre de 2026</p>

        <p class="resumen">
          Agarrao es un proyecto personal y gratuito para apuntar lo que gasta tu casa y ver
          dónde es más barato. Los precios no son nuestros: úsalos como orientación, no como
          garantía.
        </p>

        <h2>Quién ofrece esto</h2>
        <p>
          Un proyecto personal de <strong>Pimon</strong>, sin empresa detrás, sin publicidad,
          sin cobrar nada y sin acuerdos con ningún supermercado ni gasolinera. Para cualquier
          cosa: <a href="mailto:hola@pimon.dev">hola&#64;pimon.dev</a>.
        </p>

        <h2>Los precios pueden no estar al día</h2>
        <ul>
          <li>
            Los de las <strong>gasolineras</strong> vienen del Ministerio para la Transición
            Ecológica y pueden llevar hasta media hora de retraso o errores de origen.
          </li>
          <li>
            Los del <strong>súper</strong> salen de sus webs, de Open Prices y de vuestros
            tickets. En la tienda pueden ser otros (las piezas al peso, las ofertas, la tienda
            concreta).
          </li>
          <li>
            <strong>Manda el precio de la tienda o del surtidor</strong>, no el de aquí.
            Compruébalo antes de desviarte por él.
          </li>
        </ul>

        <h2>Lo que calcula la app es orientativo</h2>
        <p>
          Los gastos, el consumo del coche, el ahorro y las rutas salen de lo que se apunta y de
          datos de terceros. Si un dato está mal, el resultado también. Sirven para hacerse una
          idea, no para nada que exija exactitud.
        </p>

        <h2>Tu cuenta y tu casa</h2>
        <ul>
          <li>Se entra con tu cuenta de Google. Es para tu uso personal.</li>
          <li>
            Lo que apuntas en una casa lo ven todos sus miembros. Invita solo a quien quieras
            que lo vea.
          </li>
          <li>Tienes que tener al menos 14 años.</li>
        </ul>

        <h2>Qué no se puede hacer</h2>
        <ul>
          <li>Sacar los precios de forma automática o en masa, ni montar otro servicio con ellos.</li>
          <li>Lanzar peticiones masivas o cualquier cosa que degrade el servicio para los demás.</li>
          <li>
            Intentar entrar en casas o cuentas ajenas. Si encuentras un fallo, avisa a
            <a href="mailto:hola@pimon.dev">hola&#64;pimon.dev</a> en vez de aprovecharlo.
          </li>
        </ul>
        <p>Si una cuenta se usa para algo de esto, se puede cerrar sin aviso.</p>

        <h2>Sin garantías</h2>
        <p>
          Agarrao se ofrece <strong>tal cual</strong>: puede caerse, fallar o dejar de existir.
          Dentro de lo que la ley permite, no se responde de los daños por usarla o no poder
          usarla; lo que la ley no permite excluir (dolo o negligencia grave) sigue en pie. Esto
          no limita tus derechos como consumidor ni los de la
          <a routerLink="/privacidad">política de privacidad</a>.
        </p>

        <h2>Cambios</h2>
        <p>
          Si estas condiciones cambian, se actualiza la fecha de arriba y, si es algo
          importante, se avisa en Novedades. Se aplica la ley española.
        </p>

        <p class="pie">
          <a routerLink="/privacidad">Política de privacidad</a> · <a routerLink="/">Volver</a>
        </p>
      </article>
    </ion-content>
  `,
  styleUrl: './legal.scss',
})
export class CondicionesPage {}
