-- Idempotent overlevering av journalførte søknader fra innsendt-soeknad (utvidet omstillingsstønad).
-- Raden skrives i samme transaksjon som oppgaven, slik at retry og parallelle kall gir samme oppgave.
-- Søknadsinnholdet lagres ikke her; det ligger i innsendt-soeknad og i journalposten.
CREATE TABLE soeknad_overlevering
(
    mottaksnoekkel     TEXT PRIMARY KEY,
    soeknad_type       TEXT        NOT NULL,
    sak_id             BIGINT      NOT NULL REFERENCES sak (id),
    journalpost_id     TEXT        NOT NULL,
    dokument_info_ider JSONB       NOT NULL,
    oppgave_id         UUID,
    opprettet          TIMESTAMPTZ NOT NULL DEFAULT now()
);
