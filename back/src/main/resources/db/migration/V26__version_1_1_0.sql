-- ---------------------------------------------------------------------------
-- V26 · Se anuncia la 1.1.0: Agarrao (antes Apañaos) y la Gasolina de Gasofa dentro.
-- La mínima no cambia: la cinta de versión nueva es opcional.
-- ---------------------------------------------------------------------------
UPDATE version_app
SET ultima = '1.1.0',
    notas = 'Ahora se llama Agarrao y trae Gasolina: gasolineras baratas cerca, favoritas y repostajes.',
    actualizada_en = now()
WHERE id = 1;
