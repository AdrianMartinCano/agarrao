-- ---------------------------------------------------------------------------
-- V13 · Cruces vistos con Consum y Eroski.
--
-- «Aceite de oliva extra suave» (refinado, no virgen) casaba como aceite de oliva virgen
-- extra por la pista «aceite oliva extra». Y el papel higiénico húmedo «pure» (paquetes de
-- 80) salía a 0,04 € el rollo.
-- ---------------------------------------------------------------------------

UPDATE genericos SET pistas = pistas || ',-suave,-intenso,-orujo,-refinado'
WHERE hogar_id IS NULL AND nombre = 'Aceite de oliva virgen extra';

UPDATE genericos SET pistas = pistas || ',-pure'
WHERE hogar_id IS NULL AND nombre = 'Papel higiénico';
