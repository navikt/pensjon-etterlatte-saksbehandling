ALTER TABLE behandling ADD COLUMN kildesystem TEXT;
UPDATE behandling SET kildesystem = 'DOFFEN';
ALTER TABLE behandling ALTER COLUMN kildesystem SET NOT NULL;