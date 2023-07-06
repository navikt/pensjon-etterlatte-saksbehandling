ALTER TABLE avkortet_ytelse
    ADD COLUMN ytelse_etter_avkorting_uten_restanse BIGINT;
update avkortet_ytelse
set ytelse_etter_avkorting_uten_restanse = 0
where ytelse_etter_avkorting_uten_restanse is null;

ALTER TABLE avkortet_ytelse
    ADD COLUMN type TEXT;
update avkortet_ytelse
set type = 'NY'
where type is null;

CREATE TABLE avkorting_aarsoppgjoer_ytelse_foer_avkorting
(
    id                  UUID   NOT NULL,
    behandling_id       UUID   NOT NULL,
    beregning           BIGINT NOT NULL,
    fom                 Date   NOT NULL,
    tom                 Date,
    beregningsreferanse UUID
);

CREATE TABLE avkorting_aarsoppgjoer_restanse
(
    id               UUID   NOT NULL,
    behandling_id    UUID   NOT NULL,
    total_restanse   BIGINT NOT NULL,
    fordelt_restanse BIGINT NOT NULL,
    tidspunkt        TIMESTAMP,
    regel_resultat   TEXT,
    kilde            TEXT
);

drop table avkorting_aarsoppgjoer;