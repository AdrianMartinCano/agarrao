-- ---------------------------------------------------------------------------
-- V5 · Las cantidades guardadas de Open Food Facts antes de limpiar el signo de cantidad
-- estimada («400 g e», «1 L ℮») lo pierden. Las nuevas ya llegan limpias (OpenFoodFacts).
-- ---------------------------------------------------------------------------

UPDATE productos
SET cantidad = regexp_replace(cantidad, '[[:space:]]+(e|℮)$', '')
WHERE cantidad ~ '[[:space:]]+(e|℮)$';
