CREATE TABLE trygdetid
(
    id                  UUID PRIMARY KEY,
    behandling_id       UUID NOT NULL,
    opprettet           TIMESTAMP,
    trygdetid_nasjonal  BIGINT,
    trygdetid_fremtidig BIGINT,
    trygdetid_total     BIGINT,
);

CREATE TABLE trygdetid_grunnlag
(
    id           UUID PRIMARY KEY,
    trygdetid_id UUID references trygdetid (id) ON delete cascade,
    type         VARCHAR NOT NULL,
    periode_fra  DATE,
    periode_til  DATE,

);