CREATE TABLE inntekt_innvilget
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    grunnlag_id    UUID      NOT NULL,
    behandling_id  UUID      NOT NULL,
    inntekt        BIGINT    NOT NULL,
    regel_resultat TEXT      NOT NULL,
    kilde          TEXT      NOT NULL,
    tidspunkt      TIMESTAMP NOT NULL
)