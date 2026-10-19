-- ---------------------------------------------------------------------------
-- V16 · Los supermercados que compara cada hogar.
--
-- En Ajustes cada casa elige qué cadenas ver y comparar (las suyas, las que tiene cerca).
-- Separadas por comas («MERCADONA,LIDL»); null = todas.
-- ---------------------------------------------------------------------------

ALTER TABLE hogares ADD COLUMN cadenas varchar(300);
