-- V35 · Se anuncia la 1.2.1: el buscador de sitios ya no dice «no encontrado» mientras escribes.
UPDATE version_app
SET ultima = '1.2.1',
    notas = 'Al buscar un pueblo, ya no sale «no encontrado» mientras escribes.',
    actualizada_en = now()
WHERE id = 1;
