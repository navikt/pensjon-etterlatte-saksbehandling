CREATE TABLE vedtak
(
    id BIGSERIAL PRIMARY KEY,
    sakId BIGINT NOT NULL,
    behandlingId UUID NOT NULL,
    saksbehandlerId VARCHAR,
    avkortingsresultat TEXT,
    vilkaarsresultat TEXT,
    beregningsresultat TEXT,
    vedtakfattet boolean DEFAULT false
);