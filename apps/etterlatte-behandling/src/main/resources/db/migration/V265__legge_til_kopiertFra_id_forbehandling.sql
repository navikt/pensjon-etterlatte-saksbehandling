ALTER TABLE etteroppgjoer_behandling
    DROP COLUMN IF EXISTS kopiert_fra;

ALTER TABLE etteroppgjoer_behandling
    ADD COLUMN IF NOT EXISTS kopiert_fra UUID;
