CREATE TABLE tilbakekreving_sporing
(
    id                             UUID PRIMARY KEY,
    opprettet                      TIMESTAMP WITH TIME ZONE NOT NULL,
    endret                         TIMESTAMP WITH TIME ZONE,
    fagsystem_id                   TEXT                     NOT NULL,
    kravgrunnlag_id                TEXT                     NOT NULL,
    kravgrunnlag_payload           TEXT                     NOT NULL,
    tilbakekrevingsvedtak_request  TEXT,
    tilbakekrevingsvedtak_response TEXT
);