ALTER TABLE etteroppgjoer_behandling
    DROP COLUMN IF EXISTS kopiert_fra;

ALTER TABLE etteroppgjoer_behandling
    ADD COLUMN kopiert_fra UUID;
