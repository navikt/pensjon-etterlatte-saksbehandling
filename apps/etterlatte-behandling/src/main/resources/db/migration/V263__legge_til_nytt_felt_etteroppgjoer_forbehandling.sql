DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'etteroppgjoer_behandling'
              AND column_name = 'relatert_forbehandling_id'
        ) THEN
            ALTER TABLE etteroppgjoer_behandling
                ADD COLUMN relatert_forbehandling_id UUID;
        END IF;
    END
$$;

UPDATE etteroppgjoer_behandling
SET status = 'FERDIGSTILT'
WHERE status = 'VARSELBREV_SENDT';