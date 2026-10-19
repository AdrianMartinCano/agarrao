-- ---------------------------------------------------------------------------
-- V23 · Se anuncia la 1.0.0 (Apañaos).
--
-- Quien tenga instalada una anterior ve la cinta de «¡Versión nueva!» (opcional: la mínima
-- no cambia). Lo normal es cambiar esto desde el panel; va en una migración para que salga
-- a la vez que el código que la trae.
-- ---------------------------------------------------------------------------
UPDATE version_app
SET ultima = '1.0.0',
    notas = 'Apañaos: anota cualquier gasto, el botón + y la guía «Cómo funciona».',
    actualizada_en = now()
WHERE id = 1;
