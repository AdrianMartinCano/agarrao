// Copia la versión de package.json a src/app/core/version-app.ts y las novedades del
// CHANGELOG.md de la raíz del repo a src/app/core/novedades.ts.
//
// package.json es el ÚNICO sitio donde se escribe la versión de la app, y CHANGELOG.md el
// único donde se escriben las novedades. Este script corre solo antes de `start`, `build` y
// `test` (los scripts `pre*` de npm), así que nunca hay que tocar los ficheros generados a
// mano. Se genera un .ts con la cadena y no se importa package.json desde la app porque eso
// metería el fichero entero (dependencias incluidas) en el bundle que se descarga cada usuario.
//
// OJO Docker: la imagen del front se construye solo con la carpeta front/, así que allí no
// existe ../CHANGELOG.md. Por eso novedades.ts SE VERSIONA en git y, si no encuentra el
// CHANGELOG, el script deja el que hay.
import { existsSync, readFileSync, writeFileSync } from 'node:fs';

const { version } = JSON.parse(readFileSync(new URL('../package.json', import.meta.url), 'utf8'));

if (!/^\d+\.\d+\.\d+$/.test(version)) {
  console.error(`La versión de package.json tiene que ser x.y.z y es «${version}»`);
  process.exit(1);
}

writeFileSync(
  new URL('../src/app/core/version-app.ts', import.meta.url),
  `// Generado por tools/escribir-version.mjs desde package.json. No editar a mano.\nexport const VERSION_APP = '${version}';\n`,
);

const changelog = new URL('../../CHANGELOG.md', import.meta.url);
if (existsSync(changelog)) {
  const versiones = [];
  for (const linea of readFileSync(changelog, 'utf8').split(/\r?\n/)) {
    const cabecera = /^## (\d+\.\d+\.\d+) · (\d{4}-\d{2}-\d{2})(?: · (.+))?$/.exec(linea);
    if (cabecera) {
      versiones.push({ version: cabecera[1], fecha: cabecera[2], titulo: cabecera[3] ?? null, cambios: [] });
    } else if (linea.startsWith('- ') && versiones.length > 0) {
      versiones.at(-1).cambios.push(linea.slice(2).trim());
    }
  }
  if (versiones[0]?.version !== version) {
    console.error(`CHANGELOG.md empieza por la ${versiones[0]?.version ?? '(nada)'} y package.json dice ${version}: añade sus novedades.`);
    process.exit(1);
  }
  writeFileSync(
    new URL('../src/app/core/novedades.ts', import.meta.url),
    '// Generado por tools/escribir-version.mjs desde CHANGELOG.md. No editar a mano.\n' +
      'export interface Novedad {\n  version: string;\n  fecha: string;\n  titulo: string | null;\n  cambios: string[];\n}\n\n' +
      `export const NOVEDADES: Novedad[] = ${JSON.stringify(versiones, null, 2)};\n`,
  );
}
