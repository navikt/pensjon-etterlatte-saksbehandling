ALTER TABLE etteroppgjoer_pensjonsgivendeinntekt
    ADD COLUMN regel_resultat JSONB;
ALTER TABLE etteroppgjoer_pensjonsgivendeinntekt
    ADD COLUMN tidspunkt_beregnet timestamp;

ALTER TABLE etteroppgjoer_pensjonsgivendeinntekt
    ALTER COLUMN inntektsaar DROP NOT NULL;
ALTER TABLE etteroppgjoer_pensjonsgivendeinntekt
    ALTER COLUMN fiske_fangst_familiebarnehage DROP NOT NULL;
ALTER TABLE etteroppgjoer_pensjonsgivendeinntekt
    ALTER COLUMN skatteordning DROP NOT NULL;

ALTER TABLE etteroppgjoer_pensjonsgivendeinntekt
    ADD CONSTRAINT unique_forbehandling_id UNIQUE (forbehandling_id);

UPDATE etteroppgjoer_pensjonsgivendeinntekt
SET naeringsinntekt = naeringsinntekt + fiske_fangst_familiebarnehage
WHERE regel_resultat IS NULL;
