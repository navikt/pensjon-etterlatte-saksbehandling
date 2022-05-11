CREATE TABLE vedtak
(
    id BIGSERIAL PRIMARY KEY,
    sakId BIGINT NOT NULL UNIQUE,
    behandlingId UUID NOT NULL,
    saksbehandlerId VARCHAR,
    avkortingsresultat TEXT,
    vilkaarsresultat TEXT,
    beregningsresultat TEXT,
    vedtakfattet boolean DEFAULT false
);

CREATE UNIQUE INDEX idx_sakid_behandlingId
ON vedtak(sakId, behandlingId);