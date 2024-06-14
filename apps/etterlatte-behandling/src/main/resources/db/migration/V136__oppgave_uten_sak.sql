-- Gjøre sak_id nullable
ALTER TABLE oppgave
    ALTER COLUMN sak_id DROP NOT NULL;
