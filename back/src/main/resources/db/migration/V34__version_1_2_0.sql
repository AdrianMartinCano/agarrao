-- V34 · Se anuncia la 1.2.0: Viajes (gasolineras baratas de camino) y el dominio agarrao.pimon.dev.
UPDATE version_app
SET ultima = '1.2.0',
    notas = 'Nuevo en Gasolina: Viajes. Pon de dónde sales y adónde vas y te decimos dónde repostar más barato de camino.',
    actualizada_en = now()
WHERE id = 1;
