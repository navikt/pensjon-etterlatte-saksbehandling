-- Statistikk for etteroppgjør
CREATE VIEW etteroppgjoer_big_query_statistikk AS
SELECT es.aar, es.hendelse, es.forbehandling_status, es.maaneder_ytelse,
       es.teknisk_tid, es.utbetalt_stoenad, es.ny_brutto_stoenad, es.differanse,
       es.rettsgebyr, es.rettsgebyr_gyldig_fra, es.tilbakekreving_grense, es.etterbetaling_grense,
       es.resultat_type, es.summerte_inntekter, es.pensjonsgivende_inntekter, es.tilknyttet_revurdering,
       es.pgi_gammelt_format
FROM etteroppgjoer_statistikk es
