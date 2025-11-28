-- Nye kolonner for stønadsstatistikken på etteroppgjøret
ALTER TABLE maaned_stoenad
    ADD COLUMN etteroppgjoer_aar TEXT,
    ADD COLUMN etteroppgjoer_utbetalt BIGINT,
    ADD COLUMN etteroppgjoer_ny_stoenad BIGINT,
    ADD COLUMN etteroppgjoer_differanse BIGINT,
    ADD COLUMN etteroppgjoer_resultat TEXT,
    ADD COLUMN etterbetalt_beloep BIGINT,
    ADD COLUMN tilbakekrevd_beloep BIGINT;
