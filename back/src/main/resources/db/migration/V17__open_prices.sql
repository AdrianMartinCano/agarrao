-- ---------------------------------------------------------------------------
-- V17 · Precios de Open Prices junto a los de las webs.
--
-- De dónde sale cada precio (WEB = la web de la cadena, de hoy; OPEN_PRICES = lo que subió
-- alguien a Open Prices desde un ticket o una etiqueta) y, para los segundos, de qué día es.
-- ---------------------------------------------------------------------------

ALTER TABLE productos_tienda ADD COLUMN fuente varchar(12) NOT NULL DEFAULT 'WEB';
ALTER TABLE productos_tienda ADD COLUMN fecha_precio date;
