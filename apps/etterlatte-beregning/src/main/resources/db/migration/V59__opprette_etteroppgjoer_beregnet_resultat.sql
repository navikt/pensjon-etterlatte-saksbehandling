CREATE TABLE etteroppgjoer_beregnet_resultat
(
    id                                  UUID PRIMARY KEY,
    aar                                 INT,
    siste_iverksatte_behandling_id      UUID,
    forbehandling_id                    UUID,
    utbetalt_stoenad                    BIGINT,
    ny_brutto_stoenad                   BIGINT,
    differanse                          BIGINT,
    grense                              TEXT,
    resultat_type                       TEXT,
    tidspunkt                           TIMESTAMP,
    regel_resultat                      TEXT,
    kilde                               TEXT,
    referanse_avkorting_sist_iverksatte UUID,
    referanse_avkorting_forbehandling   UUID,
    UNIQUE (aar, referanse_avkorting_forbehandling, referanse_avkorting_sist_iverksatte)
);
