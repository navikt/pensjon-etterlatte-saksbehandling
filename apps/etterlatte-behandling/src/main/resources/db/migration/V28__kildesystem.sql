ALTER TABLE behandling ADD COLUMN kilde TEXT;
UPDATE behandling SET kilde = 'DOFFEN';
ALTER TABLE behandling ALTER COLUMN kilde SET NOT NULL;