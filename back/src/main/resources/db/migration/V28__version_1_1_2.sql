-- V28 · Se anuncia la 1.1.2: Gasolina con las fichas de Agarrao.
UPDATE version_app
SET ultima = '1.1.2',
    notas = 'Gasolina con el estilo de Agarrao: fichas de color en vez de cuadros blancos.',
    actualizada_en = now()
WHERE id = 1;
