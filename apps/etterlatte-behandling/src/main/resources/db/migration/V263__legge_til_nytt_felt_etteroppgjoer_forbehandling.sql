/* den ble lagt til i dev ved et uhell og bryter flyway */
ALTER TABLE etteroppgjoer_behandling
    DROP COLUMN IF EXISTS relatert_forbehandling_id;

ALTER TABLE etteroppgjoer_behandling
    ADD COLUMN relatert_forbehandling_id UUID;

UPDATE etteroppgjoer_behandling
SET status = 'FERDIGSTILT'
WHERE status = 'VARSELBREV_SENDT';