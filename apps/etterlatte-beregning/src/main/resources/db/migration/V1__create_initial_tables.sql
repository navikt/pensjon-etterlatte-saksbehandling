CREATE TABLE beregningsperiode (
    id UUID PRIMARY KEY,
    beregningId UUID NOT NULL,
    behandlingId UUID NOT NULL,
    beregnetDato TIMESTAMP,
    datoFOM TEXT,
    datoTOM TEXT,
    utbetaltBeloep BIGINT,
    soeskenFlokk JSONB,
    grunnbeloepMnd BIGINT,
    grunnbeloep BIGINT,
    sakId BIGINT,
    grunnlagVersjon BIGINT
);

CREATE INDEX index_name ON beregningsperiode (beregningId);
