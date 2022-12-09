CREATE TABLE vilkaarsvurdering_v2
(
    id                     UUID PRIMARY KEY,
    sak_id                 INTEGER NOT NULL,
    behandling_id          UUID    NOT NULL,
    grunnlag_versjon       INTEGER NOT NULL,
    virkningstidspunkt     DATE    NOT NULL,
    resultat_utfall        VARCHAR,
    resultat_kommentar     VARCHAR,
    resultat_tidspunkt     TIMESTAMP,
    resultat_saksbehandler VARCHAR
);

CREATE TABLE vilkaar
(
    id                     UUID PRIMARY KEY,
    vilkaarsvurdering_id   UUID references vilkaarsvurdering_v2 (id) ON delete cascade,
    resultat_kommentar     VARCHAR,
    resultat_tidspunkt     TIMESTAMP,
    resultat_saksbehandler VARCHAR
);

CREATE TABLE delvilkaar
(
    id           UUID PRIMARY KEY DEFAULT (uuid_generate_v4()),
    vilkaar_id   UUID references vilkaar (id) ON delete cascade,
    vilkaar_type VARCHAR NOT NULL,
    hovedvilkaar BOOLEAN NOT NULL,
    tittel       VARCHAR NOT NULL,
    beskrivelse  VARCHAR,
    paragraf     VARCHAR,
    ledd         INTEGER,
    bokstav      VARCHAR,
    lenke        VARCHAR,
    resultat     VARCHAR
);

CREATE TABLE grunnlag
(
    id              UUID PRIMARY KEY DEFAULT (uuid_generate_v4()),
    vilkaar_id      UUID references vilkaar (id) ON delete cascade,
    grunnlag_id     UUID    NOT NULL,
    opplysning_type VARCHAR NOT NULL,
    kilde           JSONB   NOT NULL,
    opplysning      JSONB   NOT NULL
);