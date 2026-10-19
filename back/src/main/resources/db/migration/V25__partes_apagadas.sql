-- ---------------------------------------------------------------------------
-- V25 · Se guardan las partes APAGADAS, no las encendidas.
--
-- Con las encendidas, quien ya había elegido no veía las partes nuevas (Gasolina, al
-- llegar de Gasofa). Con las apagadas, lo nuevo sale encendido para todos y cada uno
-- lo apaga si no lo quiere. Las filas de V22 se pasan con las tres partes que había.
-- ---------------------------------------------------------------------------
ALTER TABLE partes_usuario ADD COLUMN apagadas varchar(100) NOT NULL DEFAULT '';

UPDATE partes_usuario
SET apagadas = array_to_string(ARRAY(
        SELECT parte FROM unnest(ARRAY['LISTA', 'PRECIOS', 'GASTOS']) AS parte
        WHERE NOT parte = ANY (string_to_array(partes, ','))), ',');

ALTER TABLE partes_usuario DROP COLUMN partes;
