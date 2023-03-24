CREATE TABLE trygdetid
(
    id                  UUID PRIMARY KEY,
    behandling_id       UUID                                                        NOT NULL,
    opprettet           TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'UTC') NOT NULL,
    trygdetid_nasjonal  BIGINT,
    trygdetid_fremtidig BIGINT,
    trygdetid_total     BIGINT
);

CREATE TABLE trygdetid_grunnlag
(
    id           UUID PRIMARY KEY,
    trygdetid_id UUID references trygdetid (id) ON delete cascade,
    type         TEXT NOT NULL,
    bosted       TEXT NOT NULL,
    periode_fra  DATE    NOT NULL,
    periode_til  DATE    NOT NULL,
    kilde        TEXT NOT NULL
);