-- V31 · Los repostajes guardan la dirección de la gasolinera (antes solo rótulo y municipio).
-- Los anteriores se quedan a null: el servicio la saca del catálogo al leerlos.
ALTER TABLE repostajes ADD COLUMN estacion_direccion VARCHAR(200);
