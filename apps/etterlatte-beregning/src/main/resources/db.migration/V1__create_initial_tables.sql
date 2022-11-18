create TABLE beregning (
    beregningId UUID UNIQUE PRIMARY KEY NOT NULL,
    behanldingId UUID NOT NULL,
    beregnetDato TIMESTAMP,
    beregningsperioder JSONB,
    sakId BIGINT,
    versjon BIGINT
);

create TABLE beregningsperiode (
    beregningId UUID NOT NULL,
    CONSTRAINT beregning FOREIGN KEY(beregningId) REFERENCES beregning(beregningId),

    datoFOM TEXT,
    datoTOM TEXT,
    utbetaltBeloep BIGINT,
    soeskenFlokk JSONB,
    grunnbeloepMnd BIGINT,
    grunnbeloep BIGINT
);
CREATE INDEX index_name ON beregningsperiode (beregningId);
