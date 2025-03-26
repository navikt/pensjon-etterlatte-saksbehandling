CREATE TABLE avkortingsgrunnlag_faktisk
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    behandling_id       UUID   NOT NULL,
    aarsoppgjoer_id     UUID   NOT NULL,
    fom                 Date   NOT NULL,
    tom                 Date   NOT NULL,
    innvilgede_maaneder INT    NOT NULL,
    loennsinntekt       BIGINT NOT NULL,
    naeringsinntekt     BIGINT NOT NULL,
    afp                 BIGINT NOT NULL,
    utlandsinntekt      BIGINT NOT NULL,
    kilde               TEXT   NOT NULL
);

ALTER TABLE avkorting_aarsoppgjoer
    ADD COLUMN er_etteroppgjoer BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE avkortingsgrunnlag
    RENAME TO avkortingsgrunnlag_forventet;

