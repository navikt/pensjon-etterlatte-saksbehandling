ALTER TABLE etteroppgjoer ADD COLUMN har_utlandstilsnitt BOOLEAN;

UPDATE etteroppgjoer
SET har_utlandstilsnitt = FALSE;

