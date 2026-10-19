-- V32 · Se anuncia la 1.1.5: la dirección de cada repostaje.
UPDATE version_app
SET ultima = '1.1.5',
    notas = 'Cada repostaje enseña la dirección de la gasolinera.',
    actualizada_en = now()
WHERE id = 1;
