import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonContent } from '@ionic/angular';

/**
 * Política de privacidad de Agarrao (la enlaza la pantalla de Google al entrar).
 *
 * ESCRITA A PARTIR DE LO QUE EL CÓDIGO HACE, no de una plantilla (como la de Gasofa, de la
 * que sale): cada frase se corresponde con algo del repo (migraciones, llamadas a
 * terceros, lo que se guarda en el navegador). Si cambia lo que la app guarda o con quién
 * habla, esto se cambia en el mismo commit.
 *
 * Comprobado al escribirla (2026-10-13; Umami añadido en la 1.2.4, mismo día):
 *  · las fotos de tickets se leen EN EL MÓVIL (tesseract.js servido desde /tesseract) y la
 *    foto no sale de él; los PDF los lee el servidor y solo se guardan las líneas;
 *  · el servidor llama a OSRM (rutas de Viajes), al Ministerio, a Open Food Facts / Open
 *    Prices y a las webs de los súper: siempre él, nunca el navegador;
 *  · hay estadísticas de visitas con Umami (propio, sin cookies; ver src/index.html), sin la
 *    query y sin el código de /unirse; no hay cookies propias;
 *  · no hay botón de borrar la cuenta: se pide por correo (pendiente de añadir).
 */
@Component({
  selector: 'app-privacidad',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IonContent, RouterLink],
  template: `
    <ion-content class="trama">
      <article class="documento">
        <a class="documento__volver" routerLink="/">← Agarrao</a>
        <h1 class="titular">Privacidad</h1>
        <p class="actualizado">Última actualización: 13 de octubre de 2026</p>

        <p class="resumen">
          Agarrao guarda lo justo para funcionar: tu nombre y tu correo de Google, y lo que tú
          y tu casa apuntáis. No hay publicidad ni perfilado: solo se cuentan las visitas,
          sin cookies y en un servidor propio. Nada se vende ni se cede a nadie.
        </p>

        <h2>Quién es responsable</h2>
        <p>
          Agarrao es un proyecto personal de <strong>Pimon</strong>, sin empresa detrás. Para
          cualquier cosa sobre tus datos, escribe a
          <a href="mailto:hola@pimon.dev">hola&#64;pimon.dev</a>.
        </p>

        <h2>Qué se guarda, y por qué</h2>

        <h3>Sin cuenta</h3>
        <p>
          Buscar gasolineras y planear viajes no pide cuenta y no crea ninguna ficha tuya. Si
          das permiso, tu navegador comparte tu ubicación para buscar cerca de ti o salir desde
          donde estás; esa posición viaja en la consulta y no se guarda en la base de datos.
        </p>

        <h3>Con cuenta (entrar con Google)</h3>
        <ul>
          <li>
            <strong>Tu nombre y tu correo</strong>, y el identificador que Google da a tu
            cuenta, para reconocerte. Nada más de tu cuenta de Google: ni contactos, ni agenda,
            ni fotos.
          </li>
          <li>
            <strong>Tu casa</strong>: su nombre, quién está en ella y el código para invitar.
            Lo de la casa lo ven todos sus miembros.
          </li>
          <li><strong>La lista de la compra</strong> y quién apuntó cada cosa.</li>
          <li>
            <strong>Los tickets</strong>: tienda, fecha, total y sus líneas (producto y precio).
            Las fotos de tickets se leen <strong>en tu móvil</strong> y no se suben; de los PDF
            se guarda solo lo leído, no el archivo.
          </li>
          <li>
            <strong>Los gastos que anotes</strong> (concepto, importe, fecha, tipo) y los que se
            repiten cada mes.
          </li>
          <li>
            <strong>Gasolina</strong>: los coches de la casa (nombre, carburante y matrícula si
            la escribes), los repostajes y tus gasolineras favoritas.
          </li>
          <li>Tus preferencias: qué partes de la app usas y qué súper comparáis.</li>
        </ul>

        <h3>Registros del servidor</h3>
        <p>
          Como cualquier web, el servidor anota cada petición: tu <strong>dirección IP</strong>,
          la fecha y hora, la dirección pedida (que en las búsquedas de gasolineras y viajes
          <strong>incluye las coordenadas</strong>) y tu navegador. Sirven para diagnosticar
          fallos y frenar abusos, no se cruzan con tu cuenta y se borran solos: el servidor guarda
          solo los últimos (unos 30 MB) y los más antiguos van desapareciendo.
        </p>

        <h3>Estadísticas de visitas</h3>
        <p>
          Para saber cuánta gente usa Agarrao y qué partes, se cuentan las visitas con
          <strong>Umami</strong>, instalado en un servidor propio (no es Google Analytics ni
          ningún servicio de terceros). Se anota la página vista (sin lo que va detrás del
          <code>?</code> y sin el código de las invitaciones), de dónde vienes, tu navegador,
          tu sistema, el tamaño de pantalla y el país aproximado. <strong>No usa cookies</strong>,
          no guarda tu dirección IP y no se cruza con tu cuenta.
        </p>

        <h2>Cuánto tiempo</h2>
        <p>
          Mientras tengas la cuenta. Si quieres borrarla con todo lo tuyo, escribe a
          <a href="mailto:hola@pimon.dev">hola&#64;pimon.dev</a>. Lo de una casa compartida se
          queda para los demás miembros mientras la casa exista.
        </p>

        <h2>Con quién se habla</h2>
        <p>Con nadie en el sentido de vender o ceder datos. Para funcionar intervienen:</p>
        <ul>
          <li>
            <strong>Google</strong>, solo para entrar con tu cuenta; se aplica su política de
            privacidad.
          </li>
          <li>
            <strong>OpenStreetMap</strong>, que sirve las imágenes del mapa: las pide tu
            navegador, así que ve tu dirección IP.
          </li>
          <li>
            <strong>Google Maps</strong>, solo si pulsas la flecha para ir a una gasolinera. Ahí
            ya sales de Agarrao.
          </li>
          <li>
            Los <strong>precios</strong> (Ministerio para la Transición Ecológica, Open Prices y
            las webs de los supermercados) y las <strong>rutas</strong> de Viajes (OSRM, sobre
            OpenStreetMap) los pide el servidor, no tu navegador: ninguno de ellos sabe quién
            eres. A OSRM le llegan la salida y la llegada del viaje, sin nada más.
          </li>
          <li>
            El proveedor donde están alojados el servidor y las estadísticas de visitas, en
            Europa.
          </li>
        </ul>

        <h2>Por qué se puede hacer esto</h2>
        <p>
          Tus datos se tratan porque tú usas la aplicación para eso: sin ellos no hay nada que
          enseñarte. Los registros del servidor y las estadísticas de visitas, por interés legítimo en
          mantenerlo en pie, a salvo de abusos y en saber qué se usa para mejorarlo.
        </p>

        <h2>Cookies y almacenamiento</h2>
        <p>
          Agarrao <strong>no pone cookies</strong>. La única que puede aparecer es la del botón
          de Google, que la usa para su aviso de inicio de sesión. En el almacenamiento de tu
          navegador se guardan tu sesión, tu nombre, los filtros del buscador y si ya viste la
          guía; no salen de tu equipo y se borran al vaciar los datos del sitio.
        </p>

        <h2>Tus derechos</h2>
        <p>
          Puedes ver, corregir y borrar lo que apuntas desde la propia app. Para lo demás (una
          copia de tus datos, borrar la cuenta), escribe a
          <a href="mailto:hola@pimon.dev">hola&#64;pimon.dev</a>. Si crees que no se están
          tratando bien, puedes reclamar ante la Agencia Española de Protección de Datos
          (<a href="https://www.aepd.es" target="_blank" rel="noopener noreferrer">aepd.es</a>).
        </p>

        <p class="pie">
          <a routerLink="/condiciones">Condiciones de uso</a> · <a routerLink="/">Volver</a>
        </p>
      </article>
    </ion-content>
  `,
  styleUrl: './legal.scss',
})
export class PrivacidadPage {}
