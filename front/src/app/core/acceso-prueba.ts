/**
 * Usuario de prueba para entrar sin Google en local. En producción es `null` y la portada
 * no enseña el botón; la versión con datos (`acceso-prueba.development.ts`) solo entra en
 * la compilación de desarrollo por `fileReplacements` de angular.json, así que la clave no
 * llega nunca al paquete publicado.
 */
export const ACCESO_PRUEBA: { email: string; password: string } | null = null;
