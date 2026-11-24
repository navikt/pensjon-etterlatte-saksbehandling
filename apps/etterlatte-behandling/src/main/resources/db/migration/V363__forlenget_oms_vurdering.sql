CREATE TABLE forlenget_oms_vurdering
(
    behandling_id UUID PRIMARY KEY REFERENCES behandling (id),
    aktivitet     JSONB NOT NULL,
    begrunnelse   TEXT NOT NULL,
    utfall        TEXT NOT NULL CHECK (utfall IN ('INNVILGELSE', 'AVSLAG')),
    vurdert       BOOLEAN NOT NULL,
    opprettet     JSONB NOT NULL,
    endret        JSONB NOT NULL
);
