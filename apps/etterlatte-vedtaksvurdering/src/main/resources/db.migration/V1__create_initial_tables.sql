CREATE TABLE beregningsresultat (
    id UUID PRIMARY KEY,
    sakId BIGINT NOT NULL,
    behandlingId BIGINT NOT NULL,
    beregningsresultat TEXT
)

CREATE TABLE vilkaarsresultat (
    id UUID PRIMARY KEY,
    sakId BIGINT NOT NULL,
    behandlingId BIGINT NOT NULL,
    vilkaarsresultat TEXT
)

CREATE TABLE avkorting (
    id UUID PRIMARY KEY,
    sakId BIGINT NOT NULL,
    behandlingId BIGINT NOT NULL,
    beregningsresultat TEXT
)