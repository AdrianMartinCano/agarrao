-- V37 · Se anuncia la 1.2.3: cifras del coche centradas.
UPDATE version_app
SET ultima = '1.2.3',
    notas = 'En Repostajes, consumo, gastado y repostajes van centrados y del mismo ancho.',
    actualizada_en = now()
WHERE id = 1;
