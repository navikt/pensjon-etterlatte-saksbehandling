ALTER TABLE etteroppgjoer_behandling
    ADD COLUMN relatert_forbehandling_id UUID;

UPDATE etteroppgjoer_behandling
SET status = 'FERDIGSTILT'
WHERE status = 'VARSELBREV_SENDT';