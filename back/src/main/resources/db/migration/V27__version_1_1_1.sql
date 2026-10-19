-- V27 · Se anuncia la 1.1.1: entrar sin crear casa a mano y gasolina sin cuenta más a la vista.
UPDATE version_app
SET ultima = '1.1.1',
    notas = 'Entras directo sin crear una casa, y la gasolina sin cuenta sale arriba en la portada.',
    actualizada_en = now()
WHERE id = 1;
