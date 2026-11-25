CREATE TABLE forlenget_oms_vurdering
(
    behandling_id UUID PRIMARY KEY REFERENCES behandling (id),
    aktivitet     JSONB,
    begrunnelse   TEXT,
    utfall        TEXT CHECK (utfall IN ('INNVILGELSE', 'AVSLAG')),
    opprettet     JSONB NOT NULL,
    endret        JSONB NOT NULL
);
