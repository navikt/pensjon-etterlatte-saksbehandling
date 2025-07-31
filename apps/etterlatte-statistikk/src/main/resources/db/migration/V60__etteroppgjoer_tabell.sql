create table etteroppgjoer_statistikk (
    id BIGSERIAL primary key,
    forbehandling_id UUID,
    sak_id BIGINT,
    aar INT,
    hendelse TEXT,
    forbehandling_status TEXT,
    opprettet TIMESTAMP,
    maaneder_ytelse JSONB,
    teknisk_tid TIMESTAMP,
    utbetalt_stoenad BIGINT,
    ny_brutto_stoenad BIGINT,
    differanse BIGINT,
    rettsgebyr INT,
    rettsgebyr_gyldig_fra DATE,
    tilbakekreving_grense DOUBLE PRECISION,
    etterbetaling_grense DOUBLE PRECISION,
    resultat_type text
)
