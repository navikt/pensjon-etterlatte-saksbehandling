ALTER TABLE vilkaar ADD COLUMN kopiert BOOLEAN;
UPDATE vilkaar SET kopiert = false