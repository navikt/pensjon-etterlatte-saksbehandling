create TABLE beregning (
    beregningId UUID UNIQUE PRIMARY KEY NOT NULL,
    behanldingId UUID NOT NULL,
    beregnetDato TIMESTAMP,
    beregningsperioder JSONB,
    sakId BIGINT,
    versjon BIGINT
);
