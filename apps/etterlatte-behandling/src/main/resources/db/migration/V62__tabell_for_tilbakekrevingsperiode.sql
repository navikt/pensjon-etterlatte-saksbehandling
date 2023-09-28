create table tilbakekrevingsperiode
(
    id                      UUID PRIMARY KEY,
    tilbakekreving_id        UUID NOT NULL
        CONSTRAINT periode_tilbakekreving_fk_id
            REFERENCES tilbakekreving (id),
    maaned                  TEXT,
    klasse_kode             TEXT,
    klasse_type             TEXT,
    brutto_utbetaling       BIGINT,
    ny_brutto_utbetaling    BIGINT,
    skatteprosent           NUMERIC,
    beregnet_feilutbetaling BIGINT,
    brutto_tilbakekreving   BIGINT,
    netto_tilbakekreving    BIGINT,
    skatt                   BIGINT,
    skyld                   TEXT,
    resultat                TEXT,
    tilbakekrevingsprosent  BIGINT,
    rentetillegg            BIGINT
)