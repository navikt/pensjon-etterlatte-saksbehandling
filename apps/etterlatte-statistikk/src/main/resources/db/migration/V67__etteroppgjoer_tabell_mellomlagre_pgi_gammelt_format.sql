ALTER TABLE etteroppgjoer_statistikk ADD COLUMN pgi_gammelt_format JSONB;

UPDATE etteroppgjoer_statistikk SET pgi_gammelt_format = pensjonsgivende_inntekter;
UPDATE etteroppgjoer_statistikk SET pensjonsgivende_inntekter = null;
