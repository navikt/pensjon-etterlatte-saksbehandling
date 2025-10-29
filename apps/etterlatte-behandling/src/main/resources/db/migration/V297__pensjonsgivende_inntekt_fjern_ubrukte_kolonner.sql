
ALTER TABLE etteroppgjoer_pensjonsgivendeinntekt DROP COLUMN inntektsaar;
ALTER TABLE etteroppgjoer_pensjonsgivendeinntekt DROP COLUMN skatteordning;
ALTER TABLE etteroppgjoer_pensjonsgivendeinntekt DROP COLUMN fiske_fangst_familiebarnehage;

ALTER TABLE etteroppgjoer_pensjonsgivendeinntekt
    ADD CONSTRAINT pgi_forbehandling_id_fkey FOREIGN KEY (forbehandling_id) REFERENCES etteroppgjoer_behandling (id);
