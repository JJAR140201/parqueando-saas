-- Marca por empresa: color principal (#RRGGBB) y logo (data URL de imagen reducida en el cliente).
ALTER TABLE empresa ADD COLUMN color_marca character varying(7);
ALTER TABLE empresa ADD COLUMN logo text;
