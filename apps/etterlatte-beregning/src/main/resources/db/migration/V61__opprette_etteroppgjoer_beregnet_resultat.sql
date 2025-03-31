CREATE TABLE etteroppgjoer_beregnet_resultat
(
    id                                  UUID PRIMARY KEY,
    aar                                 INT NOT NULL,
    siste_iverksatte_behandling_id      UUID NOT NULL,
    forbehandling_id                    UUID NOT NULL,
    utbetalt_stoenad                    BIGINT NOT NULL,
    ny_brutto_stoenad                   BIGINT NOT NULL,
    differanse                          BIGINT NOT NULL,
    grense                              TEXT NOT NULL,
    resultat_type                       TEXT NOT NULL,
    tidspunkt                           TIMESTAMP NOT NULL,
    regel_resultat                      TEXT NOT NULL,
    kilde                               TEXT NOT NULL,
    referanse_avkorting_sist_iverksatte UUID NOT NULL,
    referanse_avkorting_forbehandling   UUID NOT NULL,
    UNIQUE (aar, siste_iverksatte_behandling_id, forbehandling_id)
);
