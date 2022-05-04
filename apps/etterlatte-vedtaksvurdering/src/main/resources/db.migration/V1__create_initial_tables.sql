CREATE TABLE vedtak (
    id UUID PRIMARY KEY,
    sakId BIGINT NOT NULL,
    behandlingId BIGINT NOT NULL,
    saksbehandlerId VARCHAR,
    avkortingsresultat TEXT
    vilkaarsresultat TEXT
    beregningsresultat TEXT
    vedtakfattet boolean DEFAULT false
)