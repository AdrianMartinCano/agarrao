-- ---------------------------------------------------------------------------
-- V7 · Lo aprendido de los tickets pasa a ser de cada hogar.
--
-- Compartido entre todos los hogares, un casado equivocado de una persona («LECHE SEMI
-- HACEND» → Pan) salía casado mal en los tickets de todo el mundo. Por hogar, cada casa
-- aprende lo suyo. Compartirlo podrá volver cuando varios hogares coincidan.
--
-- Lo aprendido hasta ahora (solo pruebas locales) se descarta: no se sabe de qué hogar era.
-- ---------------------------------------------------------------------------

DELETE FROM alias_ticket;

ALTER TABLE alias_ticket DROP CONSTRAINT alias_ticket_pkey;
ALTER TABLE alias_ticket ADD COLUMN hogar_id uuid NOT NULL;
ALTER TABLE alias_ticket ADD CONSTRAINT alias_ticket_pkey PRIMARY KEY (hogar_id, cadena, texto);
ALTER TABLE alias_ticket ADD CONSTRAINT fk_alias_ticket_hogar
    FOREIGN KEY (hogar_id) REFERENCES hogares (id) ON DELETE CASCADE;
